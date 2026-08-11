package com.himanshuProjects.disaster_damage_assessment_portal.repository.user;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    boolean existsByNameIgnoreCaseAndStateId(String name, Long stateId);

    boolean existsByNameIgnoreCaseAndStateIdAndIdNot(String name, Long stateId, Long id);

    List<District> findByStateIdOrderByAsc(Long stateId);

    Optional<District> findByNameIgnoreCaseAndStateId(String name, Long stateId);

    @Query("SELECT d FROM District d WHERE " +
            "(:search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:stateId IS NULL OR d.state.id = :stateId)")
    Page<District> searchDistricts(
            @Param("search") String search,
            @Param("stateId") Long stateId,
            Pageable pageable
    );
}
