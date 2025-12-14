package com.netflixplus.model;

public class DeleteRequest {
    private String requesterUsername;
    private String requesterPassword;
    private String toDeleteIdentifier;

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getRequesterPassword() { return requesterPassword; }
    public void setRequesterPassword(String requesterPassword) { this.requesterPassword = User.hashPassword(requesterPassword); }

    public String getToDeleteIdentifier() { return toDeleteIdentifier; }
    public void setToDeleteIdentifier(String toDeleteIdentifier) { this.toDeleteIdentifier = toDeleteIdentifier; }
}
