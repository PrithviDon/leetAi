package com.leetai.dto;

public class UserResponse {
    private String email;
    private String name;
    private String avatarUrl;
    private String role;

    public UserResponse() {}

    public UserResponse(String email, String name, String avatarUrl, String role) {
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
