package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.Movie;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
            Files.copy(uploadedInputStream, tempMp4.toPath());
            File hdFolder = new File(storagePathHD + movieId);
            File sdFolder = new File(storagePathSD + movieId);
            hdFolder.mkdirs();
            sdFolder.mkdirs();

            String hdPlaylist = new File(hdFolder, "playlist.m3u8").getAbsolutePath();
            String sdPlaylist = new File(sdFolder, "playlist.m3u8").getAbsolutePath();

            executeHLSCommand(tempMp4.getAbsolutePath(), hdPlaylist, 1920, 1080);

            executeHLSCommand(tempMp4.getAbsolutePath(), sdPlaylist, 640, 360);

            tempMp4.delete();

            try (Connection con = DB.openConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "INSERT INTO movies (movieid, title, description, file_hd, file_sd) VALUES (?, ?, ?, ?, ?)")) {

                ps.setString(1, movieId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setString(4, "/movies/1080p/" + movieId + "/playlist.m3u8");
                ps.setString(5, "/movies/360p/" + movieId + "/playlist.m3u8");
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

    private void executeHLSCommand(String input, String outputM3U8, int width, int height) throws IOException, InterruptedException {
        File outputDir = new File(outputM3U8).getParentFile();
        String cmd = String.format(
                "ffmpeg -i %s -vf scale=%d:%d -c:a aac -c:v h264 -start_number 0 -hls_time 10 -hls_list_size 0 -f hls %s",
                input, width, height, outputM3U8
        );
        Process process = Runtime.getRuntime().exec(cmd);
        process.waitFor();
    }
}
