package com.example.gps.network.dto;

public class RegisterReq {
    public String username;
    public String password;

    public RegisterReq(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
