package com.netflixplus.model;

public class DeleteRequest {
    private String requesterUsername;
    private String requesterPassword;
    private String toDeleteUsername;

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getRequesterPassword() { return requesterPassword; }
    public void setRequesterPassword(String requesterPassword) { this.requesterPassword = User.hashPassword(requesterPassword); }

    public String getToDeleteUsername() { return toDeleteUsername; }
    public void setToDeleteUsername(String toDeleteUsername) { this.toDeleteUsername = toDeleteUsername; }
}
