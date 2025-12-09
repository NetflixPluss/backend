package com.netflixplus.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Produces;

import java.util.ArrayList;
import java.util.List;

@Path("/test")
public class TestResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "API OK!";
    }

    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getStrings() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c"));
        return list;
    }
}