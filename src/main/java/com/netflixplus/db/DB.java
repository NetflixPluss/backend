package com.netflixplus.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import io.github.cdimascio.dotenv.Dotenv;

public class DB {

    private static Connection con;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String url = dotenv.get("DB_URL");
    private static final String user = dotenv.get("DB_USER");
    private static final String password = dotenv.get("DB_PASS");

    public static Connection openConnection() {
        System.out.println("Connecting to " + url);
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
            return con;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to connect to com.netflixplus.db.DB", e);
        }
    }

    public static Connection getConnection() {
        try {
            if (con == null || con.isClosed()) {
                con = openConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return con;
    }

    public static void closeConnection(Connection con) {
        try {
            if (con != null && !con.isClosed())
                con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
