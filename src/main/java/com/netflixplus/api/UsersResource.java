package com.netflixplus.api;

import com.netflixplus.db.DB;
import com.netflixplus.model.*;
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

            AuthenticationResource.requireAdminOrMaster(
                    con,
                    request.getRequesterUsername(),
                    request.getRequesterPassword()
            );

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

    @POST
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(DeleteRequest request) {
        try (Connection con = DB.openConnection()) {
            User requester = AuthenticationResource.requireAdminOrMaster(
                    con,
                    request.getRequesterUsername(),
                    request.getRequesterPassword()
            );

            PreparedStatement check = con.prepareStatement("SELECT * FROM users WHERE username = ?");
            check.setString(1, request.getToDeleteIdentifier());
            ResultSet rsCheck = check.executeQuery();
            if (!rsCheck.next()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\":\"User does not exist\"}")
                        .build();
            }

            Role requesterRole = Role.fromString(requester.getRole());
            Role targetRole = Role.fromString(rsCheck.getString("role"));

            if (requesterRole.level <= targetRole.level) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\":\"Cannot delete user with equal or higher role\"}")
                        .build();
            }

            PreparedStatement delete = con.prepareStatement(
                    "DELETE FROM users WHERE username = ?"
            );
            delete.setString(1, request.getToDeleteIdentifier());
            delete.executeUpdate();

            return Response.ok("{\"message\":\"User deleted successfully\"}").build();

        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }

    @POST
    @Path("/role")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleRequest request) {

        try (Connection con = DB.openConnection()) {

            User requester = AuthenticationResource.requireAdminOrMaster(
                    con,
                    request.getRequesterUsername(),
                    request.getRequesterPassword()
            );

            Role requesterRole = Role.fromString(requester.getRole());

            if (requesterRole == Role.USER) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\":\"Users cannot change roles\"}")
                        .build();
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT userid, role FROM users WHERE username = ?"
            );
            ps.setString(1, request.getTargetUsername());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\":\"Target user not found\"}")
                        .build();
            }

            Role targetRole = Role.fromString(rs.getString("role"));
            Role newRole = Role.fromString(request.getNewRole());

            if (requesterRole.level <= targetRole.level) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\":\"Cannot modify user with equal or higher role\"}")
                        .build();
            }

            if (newRole.level >= requesterRole.level) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\":\"Cannot assign role equal or higher than yours\"}")
                        .build();
            }

            PreparedStatement update = con.prepareStatement(
                    "UPDATE users SET role = ? WHERE username = ?"
            );
            update.setString(1, newRole.name());
            update.setString(2, request.getTargetUsername());
            update.executeUpdate();

            return Response.ok("{\"message\":\"Role updated successfully\"}").build();

        } catch (WebApplicationException e) {
            throw e;
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Server error\"}")
                    .build();
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
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
