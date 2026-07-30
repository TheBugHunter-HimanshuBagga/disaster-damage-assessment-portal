package com.himanshuProjects.disaster_damage_assessment_portal.entity.user;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "states")
public class State extends BaseEntity { // created by Super Admin

    @NotBlank(message = "State name can't be blank")
    @Size(max = 100, message = "State name cannot exceed 100 characters")
    @Column(name = "name" ,unique = true, nullable = false, length = 100)
    private String name;


    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 3, message = "State code must be between 2 and 3 characters")
    @Column(name = "code", nullable = false, unique = true, length = 3)
    private String code;
}
