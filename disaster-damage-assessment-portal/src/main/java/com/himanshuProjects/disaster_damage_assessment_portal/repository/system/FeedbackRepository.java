package com.himanshuProjects.disaster_damage_assessment_portal.repository.system;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByDisasterReportId(Long disasterReportId);

    boolean existsByUserIdAndDisasterReportId(Long userId, Long disasterReportId);

    @Query("SELECT f FROM Feedback f WHERE " +
            "(:search IS NULL OR LOWER(f.comments) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(f.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(f.disasterReport.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:rating IS NULL OR f.rating = :rating)")
    Page<Feedback> searchFeedbacks(
            @Param("search") String search,
            @Param("rating") Integer rating,
            Pageable pageable
    );
}
