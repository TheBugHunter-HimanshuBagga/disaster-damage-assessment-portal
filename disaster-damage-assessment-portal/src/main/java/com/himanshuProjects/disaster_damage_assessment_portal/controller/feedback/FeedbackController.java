package com.himanshuProjects.disaster_damage_assessment_portal.controller.feedback;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.CreateFeedbackRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.feedback.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody CreateFeedbackRequest request) {
        String email = authentication.getName();
        FeedbackResponse response = feedbackService.submitFeedback(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        FeedbackResponse response = feedbackService.getFeedbackById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<FeedbackResponse> getFeedbackByReportId(@PathVariable Long reportId) {
        FeedbackResponse response = feedbackService.getFeedbackByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<FeedbackPageResponse> searchFeedbacks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        FeedbackPageResponse response = feedbackService.searchFeedbacks(
                search, rating, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<FeedbackPageResponse> getMyFeedbacks(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String email = authentication.getName();
        FeedbackPageResponse response = feedbackService.getMyFeedbacks(
                email, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        feedbackService.deleteFeedback(id, email);
        return ResponseEntity.noContent().build();
    }
}
