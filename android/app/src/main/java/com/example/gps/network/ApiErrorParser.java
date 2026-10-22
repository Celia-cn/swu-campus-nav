package com.example.gps.network;

import androidx.annotation.Nullable;

import com.example.gps.network.dto.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import retrofit2.Response;

public final class ApiErrorParser {

    private static final Gson GSON = new Gson();

    private ApiErrorParser() {
    }

    @Nullable
    public static String message(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            String json = response.errorBody().string();
            ApiResponse<?> body = GSON.fromJson(json, new TypeToken<ApiResponse<Object>>() {}.getType());
            if (body != null && body.message != null && !body.message.isBlank()) {
                return body.message;
            }
        } catch (IOException | RuntimeException ignored) {
            // ignore
        }
        return null;
    }
}
