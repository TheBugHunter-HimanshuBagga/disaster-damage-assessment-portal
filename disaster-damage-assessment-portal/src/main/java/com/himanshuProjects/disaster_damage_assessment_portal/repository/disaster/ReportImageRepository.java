package com.himanshuProjects.disaster_damage_assessment_portal.repository.disaster;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.disaster.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {

    List<ReportImage> findByDisasterReportIdOrderByUploadedAtAsc(Long disasterReportId);

    long countByDisasterReportId(Long disasterReportId);

    void deleteByDisasterReportId(Long disasterReportId);
}
