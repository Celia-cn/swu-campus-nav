package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.LocationMediaResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.LocationMediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationMediaController {

    private final AuthService authService;
    private final LocationMediaService locationMediaService;

    public LocationMediaController(AuthService authService,
                                   LocationMediaService locationMediaService) {
        this.authService = authService;
        this.locationMediaService = locationMediaService;
    }

    @PostMapping("/{id}/images")
    public ApiResponse<LocationMediaResponse> uploadImage(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file,
                                                          HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("图片已添加", locationMediaService.addImage(id, file));
    }

    @DeleteMapping("/{id}/images")
    public ApiResponse<LocationMediaResponse> deleteImage(@PathVariable Long id,
                                                          @RequestParam("url") String url,
                                                          HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("图片已删除", locationMediaService.removeImage(id, url));
    }

    @PostMapping("/{id}/audio")
    public ApiResponse<LocationMediaResponse> uploadAudio(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file,
                                                          HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("讲解录音已更新", locationMediaService.replaceAudio(id, file));
    }

    @DeleteMapping("/{id}/audio")
    public ApiResponse<LocationMediaResponse> deleteAudio(@PathVariable Long id,
                                                          HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("讲解录音已删除", locationMediaService.deleteAudio(id));
    }

    private void ensureAdmin(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        authService.requireAdmin(user);
    }
}
