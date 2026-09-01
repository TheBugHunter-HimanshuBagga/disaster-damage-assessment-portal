package com.himanshuProjects.disaster_damage_assessment_portal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
@ConditionalOnProperty(name = "app.testcontainers.enabled", havingValue = "true", matchIfMissing = false)
public class MySqlTestContainerConfig {

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.0.36");

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("disaster_damage_assessment_portal_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--character-set-server=utf8mb4");
    }
}
