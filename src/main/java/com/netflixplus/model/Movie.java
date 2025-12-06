package com.netflixplus.model;

public class Movie {
    private int id;

    private String title;
    private String description;
    private String url360;
    private String url1080;

    public Movie() {}

    public Movie(int id, String title, String description, String url360, String url1080) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url360 = url360;
        this.url1080 = url1080;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl360() { return url360; }
    public void setUrl360(String url360) { this.url360 = url360; }

    public String getUrl1080() { return url1080; }
    public void setUrl1080(String url1080) { this.url1080 = url1080; }
}
