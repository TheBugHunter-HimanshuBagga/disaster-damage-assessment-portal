package com.himanshuProjects.disaster_damage_assessment_portal.repository.system;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.system.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
