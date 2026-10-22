package com.example.gps.network.dto;

public class LoginReq {
    public String username;
    public String password;
    public String client;

    public LoginReq(String username, String password) {
        this.username = username;
        this.password = password;
        this.client = "MOBILE";
    }
}
