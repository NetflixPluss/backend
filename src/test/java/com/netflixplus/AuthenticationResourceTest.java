package com.netflixplus;

import com.netflixplus.api.AuthenticationResource;
import com.netflixplus.db.DB;
import com.netflixplus.model.User;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthenticationResourceTest {

    private Connection conn;
    private AuthenticationResource auth;

    @BeforeEach
    void setup() throws SQLException {
        conn = DB.openConnection();
        auth = new AuthenticationResource();
    }

    @AfterEach
    void closeDB() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    void testRegister_Success() {
        User testUser = new User();
        testUser.setUsername(UUID.randomUUID().toString());
        testUser.setPassword("TestPassword");

        try (Response resp = auth.register(testUser)) {
            Assertions.assertEquals(201, resp.getStatus());
        }
    }

    @Test
    void testRegisterSameUser_Conflict() {
        String username = "user_" + UUID.randomUUID();

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword("TestPassword");

        Response r1 = auth.register(newUser);
        Assertions.assertEquals(201, r1.getStatus());

        Response r2 = auth.register(newUser);
        Assertions.assertEquals(409, r2.getStatus());
    }

    @Test
    void testSuccessfulLogin_Success() {
        User testUser = new User();
        testUser.setUsername("teste");
        testUser.setPassword("teste");
        auth.register(testUser);

        Response resp = auth.login(testUser);
        Assertions.assertEquals(200, resp.getStatus());
    }

    @Test
    void testLoginWrongPassword_Unauthorized() {
        User testUser = new User();
        testUser.setUsername(UUID.randomUUID().toString());
        testUser.setPassword("TestPassword");
        auth.register(testUser);

        testUser.setPassword("WrongPassword");

        Response resp = auth.login(testUser);
        Assertions.assertEquals(401, resp.getStatus());
    }
}