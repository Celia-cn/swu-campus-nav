package com.example.gps.network.dto;

public class ApiResponse<T> {
    public int code;
    public String message;
    public T data;
}
