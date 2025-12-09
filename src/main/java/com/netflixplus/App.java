package com.netflixplus;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class App {
    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig()
                .packages("com.netflixplus.api");
        Server server = JettyHttpContainerFactory.createServer(URI.create("http://localhost:8080/"), config);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Server started on port 8080!");

        try {
            server.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
