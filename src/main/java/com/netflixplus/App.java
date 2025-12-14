package com.netflixplus;

import com.netflixplus.model.User;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import com.netflixplus.db.DB;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.net.URI;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {

    public static void main(String[] args) {
        ensureMasterExists();
        ensureTablesExist();

        ResourceConfig config = new ResourceConfig()
                .packages("com.netflixplus.api")
                .register(MultiPartFeature.class);
        Server server = JettyHttpContainerFactory.createServer(URI.create("http://localhost:8080/"), config);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Server started on port 8080!");

        try {
            server.join();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down the application...");
                    server.stop();
                    System.out.println("Closing connection to Database");
                    DB.closeConnection();
                    System.out.println("Done, exit.");

                } catch (Exception e) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, e);
                }
            }));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureMasterExists() {
        try(Connection con = DB.openConnection()) {
            String checkSql = "SELECT COUNT(*) FROM users WHERE role='MASTER'";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO users (userid, username, password, role) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insert = con.prepareStatement(insertSql)) {
                        insert.setString(1, "master");
                        insert.setString(2, "master");
                        insert.setString(3, User.hashPassword("master123"));
                        insert.setString(4, "MASTER");
                        insert.executeUpdate();
                        System.out.println("MASTER account created: master/master123");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ensureTablesExist() {
        try (Connection con = DB.openConnection(); Statement stmt = con.createStatement()) {

            String usersSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "userid VARCHAR(50) PRIMARY KEY, " +
                    "username VARCHAR(100) UNIQUE NOT NULL, " +
                    "password VARCHAR(256) NOT NULL, " +
                    "role ENUM('USER','ADMIN','MASTER') NOT NULL DEFAULT 'USER'" +
                    ")";
            stmt.executeUpdate(usersSql);

            String moviesSql = "CREATE TABLE IF NOT EXISTS movies (" +
                    "movieid VARCHAR(36) PRIMARY KEY, " +
                    "title TEXT, " +
                    "description TEXT, " +
                    "status TEXT, " +
                    "path_hls_hd TEXT, " +
                    "path_hls_sd TEXT, " +
                    "path_mp4_hd TEXT, " +
                    "path_mp4_sd TEXT" +
                    ")";
            stmt.executeUpdate(moviesSql);

            System.out.println("Database tables ensured.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
