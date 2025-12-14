package com.netflixplus.model;

public class Movie {
    private String movieid;

    private String title;
    private String description;
    private String file_sd;
    private String file_hd;
    private String status;

    public Movie() {}

    public Movie(String movieid, String title, String description, String file_hd, String file_sd, String status) {
        this.movieid = movieid;
        this.title = title;
        this.description = description;
        this.file_hd = file_hd;
        this.file_sd = file_sd;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMovieid() {
        return movieid;
    }

    public void setMovieid(String movieid) {
        this.movieid = movieid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFile_sd() {
        return file_sd;
    }

    public void setFile_sd(String file_sd) {
        this.file_sd = file_sd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFile_hd() {
        return file_hd;
    }

    public void setFile_hd(String file_hd) {
        this.file_hd = file_hd;
    }
}
