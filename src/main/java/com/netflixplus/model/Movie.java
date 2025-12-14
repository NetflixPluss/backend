package com.netflixplus.model;

public class Movie {
    private String movieid;
    private String title;
    private String description;

    private String pathHlsHd;
    private String pathHlsSd;
    private String pathMp4Hd;
    private String pathMp4Sd;

    private String status;

    public Movie(String movieid, String title, String description,
                 String pathHlsHd, String pathHlsSd,
                 String pathMp4Hd, String pathMp4Sd,
                 String status) {
        this.movieid = movieid;
        this.title = title;
        this.description = description;
        this.pathHlsHd = pathHlsHd;
        this.pathHlsSd = pathHlsSd;
        this.pathMp4Hd = pathMp4Hd;
        this.pathMp4Sd = pathMp4Sd;
        this.status = status;
    }

    public String getMovieid() { return movieid; }
    public void setMovieid(String movieid) { this.movieid = movieid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPathHlsHd() { return pathHlsHd; }
    public void setPathHlsHd(String pathHlsHd) { this.pathHlsHd = pathHlsHd; }

    public String getPathHlsSd() { return pathHlsSd; }
    public void setPathHlsSd(String pathHlsSd) { this.pathHlsSd = pathHlsSd; }

    public String getPathMp4Hd() { return pathMp4Hd; }
    public void setPathMp4Hd(String pathMp4Hd) { this.pathMp4Hd = pathMp4Hd; }

    public String getPathMp4Sd() { return pathMp4Sd; }
    public void setPathMp4Sd(String pathMp4Sd) { this.pathMp4Sd = pathMp4Sd; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
