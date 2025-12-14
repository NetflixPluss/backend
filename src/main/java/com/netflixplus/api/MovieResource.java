package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.DeleteRequest;
import com.netflixplus.model.Movie;

import com.netflixplus.model.Role;
import com.netflixplus.model.User;
import com.netflixplus.processing.VideoProcessor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

@Path("/movies")
public class MovieResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        try (Connection con = DB.openConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM movies")) {
             while (rs.next()) {
                 movies.add(new Movie(
                         rs.getString("movieid"),
                         rs.getString("title"),
                         rs.getString("description"),
                         rs.getString("file_hd"),
                         rs.getString("file_sd"),
                         rs.getString("status")
                 ));
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    private static final String STORAGE_HD = "/var/www/netflixplus/hls/1080p/";
    private static final String STORAGE_SD = "/var/www/netflixplus/hls/360p/";
    private static final ExecutorService uploadExecutor = Executors.newFixedThreadPool(4);

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadMovie(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("title") String title,
            @FormDataParam("description") String description
    ) {
        String movieId = UUID.randomUUID().toString();
        File tempFile = new File("/tmp/" + movieId + "_" + fileDetail.getFileName());

        try {
            Files.copy(uploadedInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try (Connection con = DB.openConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "INSERT INTO movies (movieid, title, description, status) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, movieId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setString(4, "PROCESSING");
                ps.executeUpdate();
            }

            uploadExecutor.submit(() -> processVideo(movieId, tempFile));

            return Response.accepted()
                    .entity("{\"movieId\":\"" + movieId + "\",\"status\":\"PROCESSING\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("{\"error\":\"Upload failed\"}")
                    .build();
        }
    }

    private void processVideo(String movieId, File inputFile) {
        File hdDir = new File(STORAGE_HD, movieId);
        File sdDir = new File(STORAGE_SD, movieId);

        hdDir.mkdirs();
        sdDir.mkdirs();

        String hdM3u8 = new File(hdDir, "playlist.m3u8").getAbsolutePath();
        String sdM3u8 = new File(sdDir, "playlist.m3u8").getAbsolutePath();

        try {
            System.out.println("Processing video: " + movieId);

            executeHLS(inputFile.getAbsolutePath(), hdM3u8, 1920, 1080, movieId);
            executeHLS(inputFile.getAbsolutePath(), sdM3u8, 640, 360, movieId);

            try (Connection con = DB.openConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE movies SET file_hd=?, file_sd=?, status=? WHERE movieid=?")) {
                ps.setString(1, "/movies/1080p/" + movieId + "/playlist.m3u8");
                ps.setString(2, "/movies/360p/" + movieId + "/playlist.m3u8");
                ps.setString(3, "READY");
                ps.setString(4, movieId);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus(movieId, "ERROR");
            deleteFiles(movieId);
        } finally {
            inputFile.delete();
            VideoProcessor.done(movieId);
        }
    }

    private void executeHLS(
            String input,
            String output,
            int width,
            int height,
            String movieId
    ) throws Exception {

        String[] cmd = {
                "ffmpeg",
                "-y",
                "-i", input,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "28",
                "-c:a", "aac",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-f", "hls",
                output
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        VideoProcessor.registerProcess(movieId, process);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFMPEG] " + line);
            }
        }

        int exit = process.waitFor();
        if (exit != 0) throw new RuntimeException("FFmpeg failed for movie " + movieId);
    }

    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMovie(DeleteRequest request) {
        try (Connection con = DB.openConnection()) {
            User requester = AuthenticationResource.requireAdminOrMaster(
                    con,
                    request.getRequesterUsername(),
                    request.getRequesterPassword()
            );

            String movieId = request.getToDeleteIdentifier();

            System.out.println("Checking if movie exists: " + movieId);

            PreparedStatement check = con.prepareStatement("SELECT * FROM movies WHERE movieid = ?");
            check.setString(1, movieId);
            ResultSet rsCheck = check.executeQuery();
            if (!rsCheck.next()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\":\"Movie does not exist\"}")
                        .build();
            }

            PreparedStatement delete = con.prepareStatement(
                    "DELETE FROM movies WHERE movieid = ?"
            );
            delete.setString(1, movieId);
            delete.executeUpdate();

            VideoProcessor.cancel(movieId);
            deleteFiles(movieId);
            return Response.ok("{\"message\":\"Deleted\"}").build();

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }

    private void updateStatus(String movieId, String status) {
        String sql = "UPDATE movies SET status=? WHERE movieid=?";
        try (Connection con = DB.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, movieId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteFiles(String movieId) {
        File hdDir = new File(STORAGE_HD, movieId);
        File sdDir = new File(STORAGE_SD, movieId);

        if (hdDir.exists()) {
            for (File f : hdDir.listFiles()) f.delete();
            hdDir.delete();
        }

        if (sdDir.exists()) {
            for (File f : sdDir.listFiles()) f.delete();
            sdDir.delete();
        }
    }

    private void deleteDirectoryRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectoryRecursively(f);
                else f.delete();
            }
        }
        dir.delete();
    }

}
