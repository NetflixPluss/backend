package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.Movie;
import com.netflixplus.model.RegisterRequest;
import com.netflixplus.model.User;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/users")
public class UsersResource {

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegisterRequest request) {
        try (Connection con = DB.openConnection()) {

            AuthenticationResource.requireAdminOrMaster(con, request.getRequesterUsername(), request.getRequesterPassword());

            PreparedStatement check = con.prepareStatement("SELECT * FROM users WHERE username = ?");
            check.setString(1, request.getNewUser().getUsername());
            ResultSet rsCheck = check.executeQuery();
            if (rsCheck.next()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"message\":\"Username already exists\"}")
                        .build();
            }

            User newUser = request.getNewUser();
            newUser.setUserid(UUID.randomUUID().toString());

            PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO users (userid, username, password, role) VALUES (?, ?, ?, ?)"
            );
            insert.setString(1, newUser.getUserid());
            insert.setString(2, newUser.getUsername());
            insert.setString(3, newUser.getPassword());
            insert.setString(4, "USER");
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers(User authAdmin) {

        List<User> users = new ArrayList<>();

        try (Connection con = DB.openConnection()) {

            AuthenticationResource.requireAdminOrMaster(con, authAdmin.getUsername(), authAdmin.getPassword());

            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT userid, username, role FROM users");

            while (rs.next()) {
                User user = new User();
                user.setUserid(rs.getString("userid"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                users.add(user);
            }

            return Response.ok(users).build();

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }
}
