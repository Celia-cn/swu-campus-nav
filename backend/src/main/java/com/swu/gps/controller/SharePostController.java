package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.ShareCommentRequest;
import com.swu.gps.model.request.SharePostUpsertRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.ShareCommentResponse;
import com.swu.gps.model.response.SharePostResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.SharePostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class SharePostController {

    private final AuthService authService;
    private final SharePostService sharePostService;

    public SharePostController(AuthService authService, SharePostService sharePostService) {
        this.authService = authService;
        this.sharePostService = sharePostService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<SharePostResponse>> listPublic(@RequestParam String category,
                                                           HttpServletRequest request) {
        Long viewerId = authService.findUserOptional(request).map(UserAccount::getId).orElse(null);
        return ApiResponse.success(sharePostService.listPublic(category, viewerId));
    }

    @GetMapping("/posts/mine")
    public ApiResponse<List<SharePostResponse>> listMine(@RequestParam String category,
                                                         HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(sharePostService.listMine(user, category));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<SharePostResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        Long viewerId = authService.findUserOptional(request).map(UserAccount::getId).orElse(null);
        return ApiResponse.success(sharePostService.getDetail(id, viewerId));
    }

    @PostMapping("/posts")
    public ApiResponse<SharePostResponse> create(@Valid @RequestBody SharePostUpsertRequest body,
                                                 HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success("发布成功", sharePostService.create(user, body));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<SharePostResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody SharePostUpsertRequest body,
                                                 HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success("更新成功", sharePostService.update(user, id, body));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        sharePostService.delete(user, id);
        return ApiResponse.success("已删除", null);
    }

    @PostMapping("/upload/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam String category,
                                                        @RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        String url = sharePostService.uploadImage(user, category, file);
        return ApiResponse.success("上传成功", Map.of("url", url));
    }

    @PostMapping("/upload/title-image")
    public ApiResponse<Map<String, String>> uploadTitleImage(@RequestParam String category,
                                                             @RequestParam String title,
                                                             HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        String url = sharePostService.uploadTitleAsImage(user, category, title);
        return ApiResponse.success("标题图片已生成", Map.of("url", url));
    }

    @GetMapping("/posts/{id}/comments")
    public ApiResponse<List<ShareCommentResponse>> comments(@PathVariable Long id) {
        return ApiResponse.success(sharePostService.listComments(id));
    }

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<ShareCommentResponse> addComment(@PathVariable Long id,
                                                        @Valid @RequestBody ShareCommentRequest body,
                                                        HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success("评论成功", sharePostService.addComment(user, id, body));
    }

    @PostMapping("/posts/{id}/like")
    public ApiResponse<SharePostResponse> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(sharePostService.toggleLike(user, id));
    }
}
