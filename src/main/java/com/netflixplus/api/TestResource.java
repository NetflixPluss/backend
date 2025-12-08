package com.netflixplus.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Produces;

@Path("/test")
public class TestResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "API OK!";
    }
}