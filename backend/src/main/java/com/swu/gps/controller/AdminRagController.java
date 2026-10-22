package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.RagUpsertRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.RagDocumentResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.RagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rag")
public class AdminRagController {

    private final AuthService authService;
    private final RagService ragService;

    public AdminRagController(AuthService authService, RagService ragService) {
        this.authService = authService;
        this.ragService = ragService;
    }

    @GetMapping("/documents")
    public ApiResponse<List<RagDocumentResponse>> list(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(ragService.listAll());
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<RagDocumentResponse> get(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(ragService.get(id));
    }

    @PostMapping("/documents")
    public ApiResponse<RagDocumentResponse> create(@Valid @RequestBody RagUpsertRequest body,
                                                   HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("知识条目已创建", ragService.create(body));
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<RagDocumentResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody RagUpsertRequest body,
                                                   HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("知识条目已更新", ragService.update(id, body));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        ragService.delete(id);
        return ApiResponse.success("知识条目已删除", null);
    }

    private void ensureAdmin(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        authService.requireAdmin(user);
    }
}
