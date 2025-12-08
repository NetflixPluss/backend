package com.netflixplus;

import com.netflixplus.db.DB;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.server.ResourceConfig;

public class App {
    public static void main(String[] args) {
        Server server = new Server(8080);

        ResourceConfig config = new ResourceConfig();
        config.packages("com.netflixplus.api");
        config.register(org.glassfish.jersey.jackson.JacksonFeature.class);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        ServletHolder servlet = handler.addServlet(ServletContainer.class, "/*");
        servlet.setInitParameter(
                "jersey.config.server.provider.packages",
                "com.netflixplus.api"
        );

        server.setHandler(handler);
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
