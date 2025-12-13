package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.User;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

@Path("/auth")
public class AuthenticationResource {

    public static User requireAdminOrMaster(Connection con, String username, String password) throws SQLException {

        PreparedStatement auth = con.prepareStatement(
                "SELECT userid, username, role FROM users WHERE username = ? AND password = ?"
        );
        auth.setString(1, username);
        auth.setString(2, password);
        ResultSet rs = auth.executeQuery();

        if (!rs.next()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"message\":\"Invalid requester credentials\"}")
                            .build()
            );
        }

        User user = new User();
        user.setUserid(rs.getString("userid"));
        user.setUsername(rs.getString("username"));
        user.setRole(rs.getString("role"));

        if (!user.getRole().equals("ADMIN") && !user.getRole().equals("MASTER")) {
            throw new WebApplicationException(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\":\"Insufficient permissions\"}")
                            .build()
            );
        }

        return user;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(User loginUser) {
        try (Connection con = DB.openConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users where username = ?");
            ps.setString(1, loginUser.getUsername());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String role = rs.getString("role");

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
