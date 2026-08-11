package com.himanshuProjects.disaster_damage_assessment_portal.repository.user;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("SELECT s FROM State s WHERE " +
            "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<State> searchStates(@Param("search") String search, Pageable pageable);
}
