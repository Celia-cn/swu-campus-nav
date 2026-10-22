package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.RagCategory;
import com.swu.gps.model.entity.RagDocument;
import com.swu.gps.model.entity.ShareCategory;
import com.swu.gps.model.entity.SharePost;
import com.swu.gps.model.entity.SharePostComment;
import com.swu.gps.model.entity.SharePostLike;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserRole;
import com.swu.gps.model.request.ShareCommentRequest;
import com.swu.gps.model.request.SharePostUpsertRequest;
import com.swu.gps.model.response.ShareCommentResponse;
import com.swu.gps.model.response.SharePostResponse;
import com.swu.gps.repository.RagDocumentRepository;
import com.swu.gps.repository.SharePostCommentRepository;
import com.swu.gps.repository.SharePostLikeRepository;
import com.swu.gps.repository.SharePostRepository;
import com.swu.gps.util.TitleImageGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class SharePostService {

    private final SharePostRepository sharePostRepository;
    private final SharePostCommentRepository commentRepository;
    private final SharePostLikeRepository likeRepository;
    private final RagDocumentRepository ragDocumentRepository;
    private final MediaService mediaService;

    public SharePostService(SharePostRepository sharePostRepository,
                            SharePostCommentRepository commentRepository,
                            SharePostLikeRepository likeRepository,
                            RagDocumentRepository ragDocumentRepository,
                            MediaService mediaService) {
        this.sharePostRepository = sharePostRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.ragDocumentRepository = ragDocumentRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<SharePostResponse> listPublic(String category, Long viewerUserId) {
        ShareCategory cat = parseCategory(category);
        List<SharePost> posts = new ArrayList<>(sharePostRepository.findByCategoryOrderByCreatedAtDesc(cat));
        posts.sort(Comparator
                .comparing((SharePost p) -> p.getUser().getRole() != UserRole.ADMIN)
                .thenComparing(p -> p.getUser().getRole() == UserRole.ADMIN
                        ? p.getUpdatedAt()
                        : p.getCreatedAt(), Comparator.reverseOrder()));
        return posts.stream()
                .map(post -> toResponse(post, viewerUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharePostResponse> listMine(UserAccount user, String category) {
        ShareCategory cat = parseCategory(category);
        return sharePostRepository.findByUser_IdAndCategoryOrderByCreatedAtDesc(user.getId(), cat).stream()
                .map(post -> toResponse(post, user.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SharePostResponse getDetail(Long postId, Long viewerUserId) {
        SharePost post = findPost(postId);
        return toResponse(post, viewerUserId);
    }

    @Transactional
    public SharePostResponse create(UserAccount user, SharePostUpsertRequest request) {
        SharePost post = new SharePost();
        post.setUser(user);
        applyUpsert(post, request);
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        sharePostRepository.save(post);
        syncToRag(post);
        sharePostRepository.save(post);
        return toResponse(post, user.getId());
    }

    @Transactional
    public SharePostResponse update(UserAccount user, Long postId, SharePostUpsertRequest request) {
        SharePost post = findOwnedPost(user, postId);
        applyUpsert(post, request);
        post.setUpdatedAt(LocalDateTime.now());
        syncToRag(post);
        sharePostRepository.save(post);
        return toResponse(post, user.getId());
    }

    @Transactional
    public void delete(UserAccount user, Long postId) {
        SharePost post = findOwnedPost(user, postId);
        mediaService.deleteByPublicUrl(post.getImageUrl());
        if (post.getRagDocumentId() != null) {
            ragDocumentRepository.deleteById(post.getRagDocumentId());
        }
        sharePostRepository.delete(post);
    }

    @Transactional
    public String uploadImage(UserAccount user, String category, MultipartFile file) {
        return mediaService.uploadSharePostImage(user.getId(), parseCategory(category), file);
    }

    @Transactional
    public String uploadTitleAsImage(UserAccount user, String category, String title) {
        try {
            byte[] png = TitleImageGenerator.renderTitlePng(title);
            return mediaService.saveShareTitleImage(user.getId(), parseCategory(category), title, png);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "标题转图片失败");
        }
    }

    @Transactional
    public ShareCommentResponse addComment(UserAccount user, Long postId, ShareCommentRequest request) {
        SharePost post = findPost(postId);
        SharePostComment comment = new SharePostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(request.content().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        post.setUpdatedAt(LocalDateTime.now());
        sharePostRepository.save(post);
        return new ShareCommentResponse(
                comment.getId(),
                user.getId(),
                user.getUsername(),
                comment.getContent(),
                comment.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ShareCommentResponse> listComments(Long postId) {
        findPost(postId);
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId).stream()
                .map(c -> new ShareCommentResponse(
                        c.getId(),
                        c.getUser().getId(),
                        c.getUser().getUsername(),
                        c.getContent(),
                        c.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public SharePostResponse toggleLike(UserAccount user, Long postId) {
        SharePost post = findPost(postId);
        var existing = likeRepository.findByPost_IdAndUser_Id(postId, user.getId());
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            SharePostLike like = new SharePostLike();
            like.setPost(post);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        sharePostRepository.save(post);
        return toResponse(post, user.getId());
    }

    private void applyUpsert(SharePost post, SharePostUpsertRequest request) {
        post.setCategory(parseCategory(request.category()));
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setImageUrl(request.imageUrl().trim());
        post.setTitleAsImage(Boolean.TRUE.equals(request.titleAsImage()));
    }

    private void syncToRag(SharePost post) {
        RagCategory ragCategory = post.getCategory() == ShareCategory.FOOD
                ? RagCategory.USER_FOOD
                : RagCategory.USER_HOTEL;
        String ragContent = "【用户分享-" + (post.getCategory() == ShareCategory.FOOD ? "美食" : "住宿") + "】\n"
                + post.getTitle() + "\n\n" + post.getContent();
        LocalDateTime now = LocalDateTime.now();
        if (post.getRagDocumentId() == null) {
            RagDocument doc = new RagDocument();
            doc.setTitle(post.getTitle());
            doc.setCategory(ragCategory);
            doc.setContent(ragContent);
            doc.setEnabled(true);
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            ragDocumentRepository.save(doc);
            post.setRagDocumentId(doc.getId());
        } else {
            RagDocument doc = ragDocumentRepository.findById(post.getRagDocumentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "知识库条目不存在"));
            doc.setTitle(post.getTitle());
            doc.setCategory(ragCategory);
            doc.setContent(ragContent);
            doc.setUpdatedAt(now);
            ragDocumentRepository.save(doc);
        }
    }

    private SharePost findPost(Long id) {
        return sharePostRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "帖子不存在"));
    }

    private SharePost findOwnedPost(UserAccount user, Long id) {
        SharePost post = findPost(id);
        if (!post.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只能操作自己的帖子");
        }
        return post;
    }

    private ShareCategory parseCategory(String raw) {
        try {
            return ShareCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "分类仅支持 FOOD 或 HOTEL");
        }
    }

    private SharePostResponse toResponse(SharePost post, Long viewerUserId) {
        boolean liked = viewerUserId != null
                && likeRepository.existsByPost_IdAndUser_Id(post.getId(), viewerUserId);
        return new SharePostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getRole().name(),
                post.getCategory().name(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                post.isTitleAsImage(),
                post.getLikeCount(),
                post.getCommentCount(),
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
