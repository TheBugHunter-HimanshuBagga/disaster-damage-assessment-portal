package com.himanshuProjects.disaster_damage_assessment_portal.config;

import com.himanshuProjects.disaster_damage_assessment_portal.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no authentication required
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // User profile — any authenticated user can access their own profile
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/change-password").authenticated()

                        // Citizen profile — any authenticated user
                        .requestMatchers(HttpMethod.GET, "/api/users/citizen/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/citizen/profile").authenticated()

                        // User search — admin and officers only
                        .requestMatchers(HttpMethod.GET, "/api/users/search").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "FIELD_OFFICER")

                        // User by ID — admin and officers only
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "FIELD_OFFICER")

                        // Account status management — admin only
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{id}/account-status").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN")

                        // State management — SUPER_ADMIN only (write), all authenticated (read)
                        .requestMatchers(HttpMethod.GET, "/api/states/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/states").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/states/{id}").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/states/{id}").hasRole("SUPER_ADMIN")

                        // District management — SUPER_ADMIN only (write), all authenticated (read)
                        .requestMatchers(HttpMethod.GET, "/api/districts/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/districts").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/districts/{id}").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/districts/{id}").hasRole("SUPER_ADMIN")

                        // Disaster reports — citizen creates, admin/officer manage
                        .requestMatchers(HttpMethod.POST, "/api/reports").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.GET, "/api/reports/my").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.GET, "/api/reports/search").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "FIELD_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reports/{id}").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/{id}").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/reports/{id}/status").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "FIELD_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/reports/{id}/images").hasRole("CITIZEN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/{reportId}/images/{imageId}").hasRole("CITIZEN")

                        // Officer assignments — admin assigns, officer updates own
                        .requestMatchers(HttpMethod.POST, "/api/assignments/report/{reportId}").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/assignments/search").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN", "FIELD_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/assignments/my").hasRole("FIELD_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/assignments/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/assignments/report/{reportId}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/assignments/{id}/status").hasRole("FIELD_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/assignments/{id}/reassign").hasAnyRole("SUPER_ADMIN", "DISTRICT_ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                .formLogin(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
