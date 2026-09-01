package com.himanshuProjects.disaster_damage_assessment_portal.repository;

import com.himanshuProjects.disaster_damage_assessment_portal.config.MySqlTestContainerConfig;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.testutil.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MySqlTestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private int stateSeq;

    private User persistUser(String email, String phone, RoleType role) {
        State state = TestDataFactory.persistState(em, "State_" + email, String.format("S%02d", ++stateSeq));
        District district = TestDataFactory.persistDistrict(em, "Dist_" + email, state);
        User user = TestDataFactory.createUser(null, email, role);
        user.setPhoneNumber(phone);
        user.setDistrict(district);
        em.persist(user);
        em.flush();
        return user;
    }

    @Test
    @DisplayName("should return true when email exists")
    void existsByEmail_shouldReturnTrueWhenEmailExists() {
        persistUser("existing@example.com", "9876543201", RoleType.CITIZEN);

        boolean exists = userRepository.existsByEmail("existing@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should return false when email does not exist")
    void existsByEmail_shouldReturnFalseWhenEmailDoesNotExist() {
        persistUser("existing@example.com", "9876543201", RoleType.CITIZEN);

        boolean exists = userRepository.existsByEmail("nobody@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should return true when phone number exists")
    void existsByPhoneNumber_shouldReturnTrueWhenPhoneExists() {
        persistUser("phone@example.com", "9876543222", RoleType.CITIZEN);

        boolean exists = userRepository.existsByPhoneNumber("9876543222");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should return false when phone number does not exist")
    void existsByPhoneNumber_shouldReturnFalseWhenPhoneDoesNotExist() {
        persistUser("phone@example.com", "9876543222", RoleType.CITIZEN);

        boolean exists = userRepository.existsByPhoneNumber("9999999999");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should find user by email")
    void findByEmail_shouldReturnUser() {
        persistUser("findme@example.com", "9876543233", RoleType.DISTRICT_ADMIN);

        Optional<User> result = userRepository.findByEmail("findme@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("findme@example.com");
        assertThat(result.get().getRole()).isEqualTo(RoleType.DISTRICT_ADMIN);
    }

    @Test
    @DisplayName("should return empty when email not found")
    void findByEmail_shouldReturnEmptyWhenNotFound() {
        persistUser("findme@example.com", "9876543233", RoleType.DISTRICT_ADMIN);

        Optional<User> result = userRepository.findByEmail("absent@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should count users by role")
    void countByRole_shouldCountUsers() {
        persistUser("c1@example.com", "9111111111", RoleType.CITIZEN);
        persistUser("c2@example.com", "9222222222", RoleType.CITIZEN);
        persistUser("o@example.com", "9333333333", RoleType.FIELD_OFFICER);

        assertThat(userRepository.countByRole(RoleType.CITIZEN)).isEqualTo(2);
        assertThat(userRepository.countByRole(RoleType.FIELD_OFFICER)).isEqualTo(1);
        assertThat(userRepository.countByRole(RoleType.SUPER_ADMIN)).isZero();
    }

    @Test
    @DisplayName("should count users by account status")
    void countByAccountStatus_shouldCountUsers() {
        User active = persistUser("a@example.com", "9111111112", RoleType.CITIZEN);
        User suspended = persistUser("s@example.com", "9222222223", RoleType.CITIZEN);
        suspended.setAccountStatus(AccountStatus.SUSPENDED);
        em.merge(suspended);
        em.flush();

        assertThat(userRepository.countByAccountStatus(AccountStatus.ACTIVE)).isEqualTo(1);
        assertThat(userRepository.countByAccountStatus(AccountStatus.SUSPENDED)).isEqualTo(1);
    }

    @Test
    @DisplayName("should group user counts by role")
    void countGroupByRole_shouldGroupByRole() {
        persistUser("g1@example.com", "9111111121", RoleType.CITIZEN);
        persistUser("g2@example.com", "9222222221", RoleType.FIELD_OFFICER);
        persistUser("g3@example.com", "9333333331", RoleType.FIELD_OFFICER);

        var rows = userRepository.countGroupByRole();

        assertThat(rows).hasSize(2);
        boolean citizenFound = false;
        boolean officerFound = false;
        for (Object[] row : rows) {
            RoleType role = (RoleType) row[0];
            long count = (Long) row[1];
            if (role == RoleType.CITIZEN) {
                citizenFound = true;
                assertThat(count).isEqualTo(1);
            }
            if (role == RoleType.FIELD_OFFICER) {
                officerFound = true;
                assertThat(count).isEqualTo(2);
            }
        }
        assertThat(citizenFound).isTrue();
        assertThat(officerFound).isTrue();
    }

    @Test
    @DisplayName("should search users by name, role, status and district")
    void searchUsers_shouldFilterAndOrder() {
        State s1 = TestDataFactory.persistState(em, "State1", "S1");
        District d1 = TestDataFactory.persistDistrict(em, "District1", s1);
        User citizen = TestDataFactory.createUser(null, "alice@example.com", RoleType.CITIZEN);
        citizen.setPhoneNumber("9111111131");
        citizen.setFullName("Alice Citizen");
        citizen.setDistrict(d1);
        em.persist(citizen);

        User officer = TestDataFactory.createUser(null, "bob@example.com", RoleType.FIELD_OFFICER);
        officer.setPhoneNumber("9222222231");
        officer.setFullName("Bob Officer");
        officer.setDistrict(d1);
        em.persist(officer);
        em.flush();

        Page<User> bySearch = userRepository.searchUsers("Alice", null, null, null, PageRequest.of(0, 10));
        assertThat(bySearch.getContent()).hasSize(1);
        assertThat(bySearch.getContent().get(0).getFullName()).isEqualTo("Alice Citizen");

        Page<User> byRole = userRepository.searchUsers(null, RoleType.FIELD_OFFICER, null, null, PageRequest.of(0, 10));
        assertThat(byRole.getContent()).hasSize(1);
        assertThat(byRole.getContent().get(0).getFullName()).isEqualTo("Bob Officer");

        Page<User> byDistrict = userRepository.searchUsers(null, null, null, d1.getId(), PageRequest.of(0, 10));
        assertThat(byDistrict.getContent()).hasSize(2);

        Page<User> byStatus = userRepository.searchUsers(null, null, AccountStatus.BLOCKED, null, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).isEmpty();
    }
}
