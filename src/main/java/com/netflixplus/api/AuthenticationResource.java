package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.User;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.UUID;

@Path("/auth")
public class AuthenticationResource {

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(User newUser) {
        try (Connection con = DB.getConnection()) {
            PreparedStatement check = con.prepareStatement("SELECT * FROM  users WHERE username = ?");
            check.setString(1, newUser.getUsername());
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"message\":\"Username already exists\"}")
                        .build();
            }

            newUser.setUserid(UUID.randomUUID().toString());

            PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO users (userid, username, password) VALUES (?, ?, ?)"
            );
            insert.setString(1, newUser.getUserid());
            insert.setString(2, newUser.getUsername());
            insert.setString(3, newUser.getPassword());
            insert.executeUpdate();

            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\":\"Register success\"}")
                    .build();

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(User loginUser) {
        try (Connection con = DB.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users where username = ?");
            ps.setString(1, loginUser.getUsername());
            ResultSet rs = ps.executeQuery();

            System.out.println("Input: " + loginUser.getPassword());
            System.out.println("Hashed input: " + User.hashPassword(loginUser.getPassword()));

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String role = rs.getString("role");
                System.out.println("Stored: " + storedHash);

                if (storedHash.equals(loginUser.getPassword())) {
                    String jsonResponse = String.format(
                            "{\"username\":\"%s\", \"role\":\"%s\"}",
                            loginUser.getUsername(),
                            role
                    );
                    return Response.ok(jsonResponse).build();
                } else {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\":\"User not found\"}")
                        .build();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }
}
