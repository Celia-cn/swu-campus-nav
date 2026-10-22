package com.example.gps.network.dto;

public class ResetPasswordReq {
    public String username;
    public String password;

    public ResetPasswordReq(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
