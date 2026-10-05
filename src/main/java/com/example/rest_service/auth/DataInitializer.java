package com.example.rest_service.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository repository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (repository.existsByUsername("admin")) {
                return;
            }

            User admin = new User();

            admin.setUsername("admin");
            admin.setEmail("admin@university.local");

            admin.setPassword(
                passwordEncoder.encode("Admin123!")
            );

            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);

            repository.save(admin);
        };
    }
}