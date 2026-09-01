package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.StateRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StateDistrictRepositoryTest {

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private EntityManager em;

    private State persistState(String name, String code) {
        return TestDataFactory.persistState(em, name, code);
    }

    private District persistDistrict(String name, State state) {
        return TestDataFactory.persistDistrict(em, name, state);
    }

    @Test
    @DisplayName("should detect state name existence ignoring case")
    void existsByNameIgnoreCase_shouldBeCaseInsensitive() {
        persistState("Rajasthan", "RJ");

        assertThat(stateRepository.existsByNameIgnoreCase("rajasthan")).isTrue();
        assertThat(stateRepository.existsByNameIgnoreCase("RAJASTHAN")).isTrue();
        assertThat(stateRepository.existsByNameIgnoreCase("Gujarat")).isFalse();
    }

    @Test
    @DisplayName("should detect state code existence ignoring case")
    void existsByCodeIgnoreCase_shouldBeCaseInsensitive() {
        persistState("Rajasthan", "RJ");

        assertThat(stateRepository.existsByCodeIgnoreCase("rj")).isTrue();
        assertThat(stateRepository.existsByCodeIgnoreCase("Rj")).isTrue();
        assertThat(stateRepository.existsByCodeIgnoreCase("GJ")).isFalse();
    }

    @Test
    @DisplayName("should check name existence excluding a given id")
    void existsByNameIgnoreCaseAndIdNot_shouldExcludeId() {
        State state = persistState("Maharashtra", "MH");

        assertThat(stateRepository.existsByNameIgnoreCaseAndIdNot("maharashtra", state.getId())).isFalse();
        assertThat(stateRepository.existsByNameIgnoreCaseAndIdNot("maharashtra", 999L)).isTrue();
    }

    @Test
    @DisplayName("should check code existence excluding a given id")
    void existsByCodeIgnoreCaseAndIdNot_shouldExcludeId() {
        State state = persistState("Maharashtra", "MH");

        assertThat(stateRepository.existsByCodeIgnoreCaseAndIdNot("mh", state.getId())).isFalse();
        assertThat(stateRepository.existsByCodeIgnoreCaseAndIdNot("mh", 999L)).isTrue();
    }

    @Test
    @DisplayName("should search states by name or code")
    void searchStates_shouldFilterByNameOrCode() {
        persistState("Punjab", "PB");
        persistState("Uttar Pradesh", "UP");

        var byName = stateRepository.searchStates("punjab", PageRequest.of(0, 10));
        assertThat(byName.getContent()).hasSize(1);
        assertThat(byName.getContent().get(0).getName()).isEqualTo("Punjab");

        var byCode = stateRepository.searchStates("up", PageRequest.of(0, 10));
        assertThat(byCode.getContent()).hasSize(1);
        assertThat(byCode.getContent().get(0).getCode()).isEqualTo("UP");

        var all = stateRepository.searchStates(null, PageRequest.of(0, 10));
        assertThat(all.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should detect district name existence within a state")
    void existsByNameIgnoreCaseAndStateId_shouldScopeToState() {
        State s1 = persistState("Karnataka", "KA");
        State s2 = persistState("Tamil Nadu", "TN");
        persistDistrict("Bangalore", s1);

        assertThat(districtRepository.existsByNameIgnoreCaseAndStateId("bangalore", s1.getId())).isTrue();
        assertThat(districtRepository.existsByNameIgnoreCaseAndStateId("Bangalore", s1.getId())).isTrue();
        assertThat(districtRepository.existsByNameIgnoreCaseAndStateId("bangalore", s2.getId())).isFalse();
        assertThat(districtRepository.existsByNameIgnoreCaseAndStateId("Chennai", s2.getId())).isFalse();
    }

    @Test
    @DisplayName("should check district name existence excluding a given district id")
    void existsByNameIgnoreCaseAndStateIdAndIdNot_shouldExcludeId() {
        State s1 = persistState("Karnataka", "KA");
        District d1 = persistDistrict("Bangalore", s1);

        assertThat(districtRepository.existsByNameIgnoreCaseAndStateIdAndIdNot("bangalore", s1.getId(), d1.getId())).isFalse();
        assertThat(districtRepository.existsByNameIgnoreCaseAndStateIdAndIdNot("bangalore", s1.getId(), 999L)).isTrue();
    }

    @Test
    @DisplayName("should find districts by state id ordered by id ascending")
    void findByStateIdOrderByIdAsc_shouldOrderById() {
        State s1 = persistState("Kerala", "KL");
        State s2 = persistState("Goa", "GA");
        persistDistrict("Ernakulam", s1);
        persistDistrict("Thiruvananthapuram", s1);
        persistDistrict("Panaji", s2);

        List<District> districts = districtRepository.findByStateIdOrderByIdAsc(s1.getId());

        assertThat(districts).hasSize(2);
        assertThat(districts).extracting(District::getState).extracting(State::getId).containsOnly(s1.getId());
        assertThat(districts.get(0).getId()).isLessThan(districts.get(1).getId());
    }

    @Test
    @DisplayName("should find district by name and state id")
    void findByNameIgnoreCaseAndStateId_shouldFindDistrict() {
        State s1 = persistState("Kerala", "KL");
        persistDistrict("Ernakulam", s1);

        Optional<District> found = districtRepository.findByNameIgnoreCaseAndStateId("ernakulam", s1.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ernakulam");

        Optional<District> notFound = districtRepository.findByNameIgnoreCaseAndStateId("unknown", s1.getId());
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("should search districts by search text and state")
    void searchDistricts_shouldFilterBySearchAndState() {
        State s1 = persistState("Kerala", "KL");
        State s2 = persistState("Goa", "GA");
        persistDistrict("Kochi", s1);
        persistDistrict("Kozhikode", s1);
        persistDistrict("Margao", s2);

        var bySearch = districtRepository.searchDistricts("ko", null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(2);

        var byState = districtRepository.searchDistricts(null, s1.getId(), PageRequest.of(0, 10));
        assertThat(byState.getContent()).hasSize(2);
        assertThat(byState.getContent()).extracting(District::getName).containsExactlyInAnyOrder("Kochi", "Kozhikode");

        var combined = districtRepository.searchDistricts("koch", s1.getId(), PageRequest.of(0, 10));
        assertThat(combined.getContent()).hasSize(1);
        assertThat(combined.getContent().get(0).getName()).isEqualTo("Kochi");
    }
}
