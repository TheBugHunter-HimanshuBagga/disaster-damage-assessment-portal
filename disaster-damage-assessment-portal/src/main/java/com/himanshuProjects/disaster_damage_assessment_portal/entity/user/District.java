package com.himanshuProjects.disaster_damage_assessment_portal.entity.user;

import com.himanshuProjects.disaster_damage_assessment_portal.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "districts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name","state_id"}) // district name can't be used inside the same state
        }
)
public class District extends BaseEntity {

    @NotBlank(message = "District name is required")
    @Size(max = 100, message = "District name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100) // database column named as "name"
    private String name;

    @ManyToOne(fetch = FetchType.LAZY) // many districts comes under 1 state
    @JoinColumn(name = "state_id", nullable = false)
    private State state;
}
