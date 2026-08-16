package com.himanshuProjects.disaster_damage_assessment_portal.controller.feedback;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.CreateFeedbackRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.service.feedback.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Feedback", description = "Citizen feedback on completed disaster reports")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(summary = "Submit feedback", description = "Citizen submits feedback with rating and comment for a completed/approved report. One feedback per report.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback submitted"),
            @ApiResponse(responseCode = "400", description = "Validation error or report not eligible"),
            @ApiResponse(responseCode = "409", description = "Feedback already exists for this report")
    })
    public ResponseEntity<FeedbackResponse> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody CreateFeedbackRequest request) {
        String email = authentication.getName();
        FeedbackResponse response = feedbackService.submitFeedback(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feedback by ID", description = "Returns a specific feedback record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback found"),
            @ApiResponse(responseCode = "404", description = "Feedback not found")
    })
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        FeedbackResponse response = feedbackService.getFeedbackById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get feedback by report ID", description = "Returns the feedback for a specific disaster report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback found"),
            @ApiResponse(responseCode = "404", description = "Feedback not found for this report")
    })
    public ResponseEntity<FeedbackResponse> getFeedbackByReportId(@PathVariable Long reportId) {
        FeedbackResponse response = feedbackService.getFeedbackByReportId(reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Search feedbacks", description = "Search and filter feedbacks by rating. Supports pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
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
    @Operation(summary = "Get my feedbacks", description = "Returns all feedbacks submitted by the authenticated citizen.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedbacks returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
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
    @Operation(summary = "Delete feedback", description = "Deletes feedback. Only the feedback author can delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Feedback deleted"),
            @ApiResponse(responseCode = "403", description = "Not the feedback author"),
            @ApiResponse(responseCode = "404", description = "Feedback not found")
    })
    public ResponseEntity<Void> deleteFeedback(
            Authentication authentication,
            @PathVariable Long id) {
        String email = authentication.getName();
        feedbackService.deleteFeedback(id, email);
        return ResponseEntity.noContent().build();
    }
}
