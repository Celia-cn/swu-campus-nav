package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.RagCategory;
import com.swu.gps.model.entity.RagDocument;
import com.swu.gps.model.request.RagUpsertRequest;
import com.swu.gps.model.response.RagDocumentResponse;
import com.swu.gps.repository.RagDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
public class RagService {

    private final RagDocumentRepository ragDocumentRepository;
    private final int topK;

    public RagService(RagDocumentRepository ragDocumentRepository,
                      @Value("${app.ai.rag-top-k:5}") int topK) {
        this.ragDocumentRepository = ragDocumentRepository;
        this.topK = topK;
    }

    @Transactional(readOnly = true)
    public List<RagDocumentResponse> listAll() {
        return ragDocumentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RagDocumentResponse get(Long id) {
        RagDocument doc = ragDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "知识条目不存在"));
        return toResponse(doc);
    }

    @Transactional
    public RagDocumentResponse create(RagUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();
        RagDocument doc = new RagDocument();
        applyUpsert(doc, request);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        return toResponse(ragDocumentRepository.save(doc));
    }

    @Transactional
    public RagDocumentResponse update(Long id, RagUpsertRequest request) {
        RagDocument doc = ragDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "知识条目不存在"));
        applyUpsert(doc, request);
        doc.setUpdatedAt(LocalDateTime.now());
        return toResponse(ragDocumentRepository.save(doc));
    }

    @Transactional
    public void delete(Long id) {
        if (!ragDocumentRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "知识条目不存在");
        }
        ragDocumentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<RagDocument> retrieveForQuery(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> tokens = tokenize(normalized);
        List<RagDocument> candidates = new ArrayList<>();
        for (String token : tokens) {
            candidates.addAll(ragDocumentRepository.searchEnabled(token));
        }
        if (candidates.isEmpty()) {
            candidates.addAll(ragDocumentRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(RagDocument::isEnabled)
                    .limit(topK * 2L)
                    .collect(Collectors.toList()));
        }
        return candidates.stream()
                .collect(Collectors.toMap(RagDocument::getId, d -> d, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingInt((RagDocument doc) -> score(doc, tokens)).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private int score(RagDocument doc, Set<String> tokens) {
        String haystack = (doc.getTitle() + " " + doc.getContent()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                score += token.length() >= 2 ? 3 : 1;
            }
        }
        if (doc.getCategory() == RagCategory.ROUTE || doc.getCategory() == RagCategory.BUS) {
            if (containsAny(haystack, tokens, "去", "怎么走", "路线", "校", "车", "公交", "地铁")) {
                score += 2;
            }
        }
        return score;
    }

    private boolean containsAny(String haystack, Set<String> tokens, String... extra) {
        for (String word : extra) {
            if (haystack.contains(word) || tokens.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> tokenize(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        tokens.add(query);
        Arrays.stream(query.split("[\\s,，。！？；;、]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .forEach(tokens::add);
        for (int len = 2; len <= 4 && len <= query.length(); len++) {
            for (int i = 0; i + len <= query.length(); i++) {
                tokens.add(query.substring(i, i + len));
            }
        }
        return tokens;
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private void applyUpsert(RagDocument doc, RagUpsertRequest request) {
        doc.setTitle(request.title().trim());
        doc.setCategory(parseCategory(request.category()));
        doc.setContent(request.content().trim());
        doc.setEnabled(Boolean.TRUE.equals(request.enabled()));
    }

    private RagCategory parseCategory(String raw) {
        try {
            return RagCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "分类无效，可选：HISTORY, NEWS, FOOD, BUS, ROUTE, GENERAL, USER_FOOD, USER_HOTEL");
        }
    }

    private RagDocumentResponse toResponse(RagDocument doc) {
        return new RagDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory().name(),
                doc.getContent(),
                doc.isEnabled(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
