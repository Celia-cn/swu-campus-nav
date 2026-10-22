package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.MediaUploadResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/media")
public class MediaController {

    private final AuthService authService;
    private final MediaService mediaService;

    public MediaController(AuthService authService, MediaService mediaService) {
        this.authService = authService;
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ApiResponse<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("type") String type,
                                                   HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        authService.requireAdmin(user);
        String url = mediaService.upload(file, type);
        return ApiResponse.success("上传成功", new MediaUploadResponse(url));
    }
}
