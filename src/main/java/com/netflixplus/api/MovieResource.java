package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.Movie;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
                         rs.getInt("id"),
                         rs.getString("title"),
                         rs.getString("description"),
                         rs.getString("url360"),
                         rs.getString("url1080")
                 ));
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movies;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Movie getMovieById(@PathParam("id") int id) {
        try (Connection con = DB.openConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM movies WHERE id = ?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("url360"),
                        rs.getString("url1080")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String storagePathHD = "/var/www/netflixplus/hls/1080p/";
    private static final String storagePathSD = "/var/www/netflixplus/hls/360p/";

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
        File tempMp4 = new File("/tmp/" + fileDetail.getFileName());

        try {
            Files.copy(uploadedInputStream, tempMp4.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File hdFolder = new File(storagePathHD);
            File sdFolder = new File(storagePathSD);
            hdFolder.mkdirs();
            sdFolder.mkdirs();

            String hdPlaylist = new File(hdFolder, movieId+".m3u8").getAbsolutePath();
            String sdPlaylist = new File(sdFolder, movieId+".m3u8").getAbsolutePath();

            executeHLSCommand(tempMp4.getAbsolutePath(), hdPlaylist, 1920, 1080);

            executeHLSCommand(tempMp4.getAbsolutePath(), sdPlaylist, 640, 360);

            tempMp4.delete();

            try (Connection con = DB.openConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "INSERT INTO movies (movieid, title, description, file_hd, file_sd) VALUES (?, ?, ?, ?, ?)")) {

                ps.setString(1, movieId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setString(4, "/movies/1080p/" + movieId + ".m3u8");
                ps.setString(5, "/movies/360p/" + movieId + ".m3u8");
                ps.executeUpdate();
            }

            return Response.ok("{\"message\":\"Upload successful\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Upload failed\"}")
                    .build();
        }
    }

    private void executeHLSCommand(String input, String outputM3U8, int width, int height)
            throws IOException, InterruptedException {

        File outputDir = new File(outputM3U8).getParentFile();

        System.out.println("==== HLS DEBUG ====");
        System.out.println("Input file: " + input);
        System.out.println("Output playlist: " + outputM3U8);
        System.out.println("Output dir exists: " + outputDir.exists());
        System.out.println("Output dir writable: " + outputDir.canWrite());
        System.out.println("Running as user: " + System.getProperty("user.name"));

        String videoBitrate = (height == 1080) ? "3000k" : "800k";
        String audioBitrate = (height == 1080) ? "128k" : "96k";

        String[] cmd = {
                "ffmpeg",
                "-y",
                "-i", input,

                "-vf", "scale=" + width + ":" + height + ":flags=fast_bilinear",

                "-c:v", "libx264",
                "-preset", "veryfast",
                "-profile:v", "main",
                "-level", "4.0",
                "-b:v", videoBitrate,
                "-maxrate", videoBitrate,
                "-bufsize", "6000k",
                "-g", "48",
                "-sc_threshold", "0",

                "-c:a", "aac",
                "-b:a", audioBitrate,

                "-hls_time", "6",
                "-hls_list_size", "6",
                "-hls_flags", "delete_segments+append_list",

                "-f", "hls",
                outputM3U8
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFMPEG] " + line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println("FFmpeg exited with code: " + exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }
    }

}
