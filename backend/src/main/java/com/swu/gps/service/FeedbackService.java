package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.FeedbackStatus;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserFeedback;
import com.swu.gps.model.request.FeedbackSubmitRequest;
import com.swu.gps.model.response.FeedbackResponse;
import com.swu.gps.model.response.FeedbackStatsResponse;
import com.swu.gps.repository.UserFeedbackRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final UserFeedbackRepository userFeedbackRepository;

    public FeedbackService(UserFeedbackRepository userFeedbackRepository) {
        this.userFeedbackRepository = userFeedbackRepository;
    }

    @Transactional
    public FeedbackResponse submit(UserAccount user, FeedbackSubmitRequest request) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUser(user);
        feedback.setContent(request.content().trim());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.PENDING);
        UserFeedback saved = userFeedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listAll() {
        return userFeedbackRepository.findAllWithUserOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackStatsResponse getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long totalCount = userFeedbackRepository.count();
        long todayCount = userFeedbackRepository.countByCreatedAtGreaterThanEqual(startOfDay);
        long pendingCount = userFeedbackRepository.countByStatus(FeedbackStatus.PENDING);
        long completedCount = userFeedbackRepository.countByStatus(FeedbackStatus.COMPLETED);
        return new FeedbackStatsResponse(totalCount, todayCount, pendingCount, completedCount);
    }

    @Transactional
    public FeedbackResponse markCompleted(Long id) {
        UserFeedback feedback = userFeedbackRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "反馈不存在"));
        if (feedback.getStatus() == FeedbackStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该反馈已标记为已完成");
        }
        feedback.setStatus(FeedbackStatus.COMPLETED);
        feedback.setCompletedAt(LocalDateTime.now());
        UserFeedback saved = userFeedbackRepository.save(feedback);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!userFeedbackRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "反馈不存在");
        }
        userFeedbackRepository.deleteById(id);
    }

    private FeedbackResponse toResponse(UserFeedback feedback) {
        UserAccount user = feedback.getUser();
        return new FeedbackResponse(
                feedback.getId(),
                user.getId(),
                user.getUsername(),
                feedback.getContent(),
                feedback.getStatus().name(),
                feedback.getCreatedAt(),
                feedback.getCompletedAt()
        );
    }
}
