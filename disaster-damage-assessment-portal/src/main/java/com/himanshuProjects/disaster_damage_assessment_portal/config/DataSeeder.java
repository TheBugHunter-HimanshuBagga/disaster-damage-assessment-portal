package com.himanshuProjects.disaster_damage_assessment_portal.config;

import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.District;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.State;
import com.himanshuProjects.disaster_damage_assessment_portal.entity.user.User;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.AccountStatus;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.Gender;
import com.himanshuProjects.disaster_damage_assessment_portal.enums.RoleType;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.DistrictRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.StateRepository;
import com.himanshuProjects.disaster_damage_assessment_portal.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the master data (states, districts) and the demo accounts the UI needs
 * on a fresh database. Runs only when the users table is empty, so it is safe to
 * leave enabled in every environment.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(StateRepository stateRepository,
                      DistrictRepository districtRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder skipped - database already contains users");
            return;
        }

        log.info("Seeding states, districts and demo accounts...");

        Map<String, String> stateCodes = new LinkedHashMap<>();
        stateCodes.put("Punjab", "PB");
        stateCodes.put("Maharashtra", "MH");
        stateCodes.put("Kerala", "KL");
        stateCodes.put("Assam", "AS");
        stateCodes.put("Odisha", "OD");

        Map<String, List<String>> masterData = new LinkedHashMap<>();
        masterData.put("Punjab", List.of("Ludhiana", "Amritsar", "Jalandhar", "Mohali"));
        masterData.put("Maharashtra", List.of("Mumbai", "Pune", "Nagpur", "Nashik"));
        masterData.put("Kerala", List.of("Thiruvananthapuram", "Kochi", "Kozhikode"));
        masterData.put("Assam", List.of("Guwahati", "Silchar", "Dibrugarh"));
        masterData.put("Odisha", List.of("Bhubaneswar", "Cuttack", "Rourkela"));

        Map<String, State> states = new LinkedHashMap<>();
        masterData.forEach((stateName, districts) -> {
            State state = new State();
            state.setName(stateName);
            state.setCode(stateCodes.get(stateName));
            states.put(stateName, stateRepository.save(state));

            for (String districtName : districts) {
                District district = new District();
                district.setName(districtName);
                district.setState(state);
                districtRepository.save(district);
            }
        });

        District firstDistrict = districtRepository
                .findByStateIdOrderByIdAsc(states.get("Punjab").getId())
                .get(0);

        createUser("Super Admin", "superadmin@portal.gov.in", "9876543210",
                "Admin@1234", Gender.MALE, RoleType.SUPER_ADMIN, firstDistrict);
        createUser("District Admin", "districtadmin@portal.gov.in", "9876543211",
                "Admin@1234", Gender.FEMALE, RoleType.DISTRICT_ADMIN, firstDistrict);
        createUser("Field Officer", "officer@portal.gov.in", "9876543212",
                "Officer@1234", Gender.MALE, RoleType.FIELD_OFFICER, firstDistrict);
        createUser("Field Officer Two", "officer2@portal.gov.in", "9876543213",
                "Officer@1234", Gender.FEMALE, RoleType.FIELD_OFFICER, firstDistrict);
        createUser("Demo Citizen", "citizen@portal.gov.in", "9876543214",
                "Citizen@1234", Gender.MALE, RoleType.CITIZEN, firstDistrict);

        log.info("Seed complete: {} states, {} districts, {} users",
                stateRepository.count(), districtRepository.count(), userRepository.count());
        log.info("Demo logins -> superadmin@portal.gov.in/Admin@1234 | " +
                "districtadmin@portal.gov.in/Admin@1234 | officer@portal.gov.in/Officer@1234 | " +
                "citizen@portal.gov.in/Citizen@1234");
    }

    private void createUser(String fullName, String email, String phoneNumber,
                             String rawPassword, Gender gender, RoleType role,
                             District district) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setGender(gender);
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setDistrict(district);
        userRepository.save(user);
    }
}
