package com.example.gps.network;

import android.content.Context;

import com.example.gps.storage.SessionManager;

import java.util.concurrent.TimeUnit; // 【新增1】导入 TimeUnit 用于设置超时时间

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static Retrofit retrofit;
    private static OkHttpClient httpClient;
    private static String currentBaseUrl = "";

    private ApiClient() {
    }

    public static void reset() {
        retrofit = null;
        httpClient = null;
        currentBaseUrl = "";
    }

    public static ApiService service(Context context) {
        String baseUrl = SessionManager.from(context).getBaseUrl();
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;
            httpClient = buildHttpClient(context);
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static OkHttpClient httpClient(Context context) {
        String baseUrl = SessionManager.from(context).getBaseUrl();
        if (httpClient == null || !baseUrl.equals(currentBaseUrl)) {
            service(context);
        }
        return httpClient;
    }

    private static OkHttpClient buildHttpClient(Context context) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = SessionManager.from(context).getToken();
            if (token == null || token.isEmpty()) {
                return chain.proceed(original);
            }
            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(request);
        };

        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // 【新增2】连接超时设为 60 秒
                .readTimeout(60, TimeUnit.SECONDS)    // 【新增3】读取超时设为 60 秒（等待 AI 回复的关键）
                .writeTimeout(60, TimeUnit.SECONDS)   // 【新增4】写入超时设为 60 秒
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();
    }
}
