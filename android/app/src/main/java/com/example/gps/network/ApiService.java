package com.example.gps.network;

import com.example.gps.network.dto.AdminStatsDto;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.AuthDto;
import com.example.gps.network.dto.CheckInDto;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.network.dto.LocationUpsertReq;
import com.example.gps.network.dto.LoginReq;
import com.example.gps.network.dto.ManualPlayReq;
import com.example.gps.network.dto.MultiStopRouteReq;
import com.example.gps.network.dto.RegisterReq;
import com.example.gps.network.dto.ResetPasswordReq;
import com.example.gps.network.dto.RoutePlanDto;
import com.example.gps.network.dto.RoutePlanReq;
import com.example.gps.network.dto.TriggerHistoryDto;
import com.example.gps.network.dto.UserDto;
import com.example.gps.network.dto.UserRouteDetailDto;
import com.example.gps.network.dto.UserRouteSummaryDto;
import com.example.gps.network.dto.UserStatsDto;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/register")
    Call<ApiResponse<AuthDto>> register(@Body RegisterReq body);

    @POST("api/auth/login")
    Call<ApiResponse<AuthDto>> login(@Body LoginReq body);

    @POST("api/auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordReq body);

    @GET("api/auth/me")
    Call<ApiResponse<UserDto>> me();

    @GET("api/auth/me/stats")
    Call<ApiResponse<UserStatsDto>> myStats();

    @GET("api/locations")
    Call<ApiResponse<List<LocationDto>>> locations();

    @POST("api/locations/check-in")
    Call<ApiResponse<CheckInDto>> checkIn(@Body com.example.gps.network.dto.CheckInReq body);

    @POST("api/locations/{id}/play")
    Call<ApiResponse<Void>> manualPlay(@Path("id") long id, @Body ManualPlayReq body);

    @POST("api/locations/{id}/like")
    Call<ApiResponse<com.example.gps.network.dto.LocationLikeDto>> toggleLocationLike(
            @Path("id") long id,
            @Body java.util.Map<String, String> body);

    @POST("api/feedback")
    Call<ApiResponse<Void>> submitFeedback(@Body com.example.gps.network.dto.FeedbackSubmitReq body);

    @POST("api/ai/chat")
    Call<ApiResponse<com.example.gps.network.dto.AiChatDto>> aiChat(@Body com.example.gps.network.dto.AiChatReq body);

    @GET("api/admin/stats")
    Call<ApiResponse<AdminStatsDto>> stats();

    @POST("api/admin/locations")
    Call<ApiResponse<LocationDto>> createLocation(@Body LocationUpsertReq body);

    @PUT("api/admin/locations/{id}")
    Call<ApiResponse<LocationDto>> updateLocation(@Path("id") long id, @Body LocationUpsertReq body);

    @DELETE("api/admin/locations/{id}")
    Call<ApiResponse<Void>> deleteLocation(@Path("id") long id);

    // ==================== 路线规划相关 ====================

    @POST("api/routes/plan")
    Call<ApiResponse<RoutePlanDto>> planRoute(@Body RoutePlanReq body);

    @POST("api/routes/plan-multi-stop")
    Call<ApiResponse<RoutePlanDto>> planMultiStopRoute(@Body MultiStopRouteReq body);

    @GET("api/routes/my-routes")
    Call<ApiResponse<List<UserRouteSummaryDto>>> getMyRoutes();

    @POST("api/routes/save")
    Call<ApiResponse<UserRouteSummaryDto>> saveRoute(@Body Object body);

    @GET("api/routes/{id}")
    Call<ApiResponse<UserRouteDetailDto>> getRouteDetail(@Path("id") long id);

    @DELETE("api/routes/{id}")
    Call<ApiResponse<Void>> deleteRoute(@Path("id") long id);

    // ==================== 触发历史相关 ====================

    @GET("api/locations/{id}/trigger-history")
    Call<ApiResponse<List<TriggerHistoryDto>>> getTriggerHistory(
            @Path("id") long id,
            @Query("limit") int limit);

    // ==================== 分享社区 ====================

    @GET("api/share/posts")
    Call<ApiResponse<List<com.example.gps.network.dto.SharePostDto>>> sharePosts(@Query("category") String category);

    @GET("api/share/posts/mine")
    Call<ApiResponse<List<com.example.gps.network.dto.SharePostDto>>> mySharePosts(@Query("category") String category);

    @GET("api/share/posts/{id}")
    Call<ApiResponse<com.example.gps.network.dto.SharePostDto>> sharePostDetail(@Path("id") long id);

    @POST("api/share/posts")
    Call<ApiResponse<com.example.gps.network.dto.SharePostDto>> createSharePost(@Body com.example.gps.network.dto.SharePostUpsertReq body);

    @PUT("api/share/posts/{id}")
    Call<ApiResponse<com.example.gps.network.dto.SharePostDto>> updateSharePost(@Path("id") long id,
                                                                                  @Body com.example.gps.network.dto.SharePostUpsertReq body);

    @DELETE("api/share/posts/{id}")
    Call<ApiResponse<Void>> deleteSharePost(@Path("id") long id);

    @GET("api/share/posts/{id}/comments")
    Call<ApiResponse<List<com.example.gps.network.dto.ShareCommentDto>>> shareComments(@Path("id") long id);

    @POST("api/share/posts/{id}/comments")
    Call<ApiResponse<com.example.gps.network.dto.ShareCommentDto>> addShareComment(
            @Path("id") long id,
            @Body java.util.Map<String, String> body);

    @POST("api/share/posts/{id}/like")
    Call<ApiResponse<com.example.gps.network.dto.SharePostDto>> toggleShareLike(@Path("id") long id);

    @Multipart
    @POST("api/share/upload/image")
    Call<ApiResponse<java.util.Map<String, String>>> uploadShareImage(
            @Query("category") String category,
            @Part MultipartBody.Part file);

    @POST("api/share/upload/title-image")
    Call<ApiResponse<java.util.Map<String, String>>> uploadShareTitleImage(
            @Query("category") String category,
            @Query("title") String title);
}