package com.himanshuProjects.disaster_damage_assessment_portal.service.feedback;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.CreateFeedbackRequest;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackPageResponse;
import com.himanshuProjects.disaster_damage_assessment_portal.dto.feedback.FeedbackResponse;

public interface FeedbackService {

    FeedbackResponse submitFeedback(String citizenEmail, CreateFeedbackRequest request);

    FeedbackResponse getFeedbackById(Long id);

    FeedbackResponse getFeedbackByReportId(Long reportId);

    FeedbackPageResponse searchFeedbacks(String search, Integer rating,
                                          int page, int size,
                                          String sortBy, String sortDirection);

    FeedbackPageResponse getMyFeedbacks(String citizenEmail, int page, int size,
                                         String sortBy, String sortDirection);

    void deleteFeedback(Long id, String citizenEmail);
}
