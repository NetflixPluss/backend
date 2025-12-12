package com.netflixplus.model;

public class RegisterRequest {
    private String requesterUsername;
    private String requesterPassword;
    private User newUser;

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getRequesterPassword() { return requesterPassword; }
    public void setRequesterPassword(String requesterPassword) { this.requesterPassword = User.hashPassword(requesterPassword); }

    public User getNewUser() { return newUser; }
    public void setNewUser(User newUser) { this.newUser = newUser; }
}
