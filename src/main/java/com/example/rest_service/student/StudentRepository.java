package com.example.rest_service.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StudentRepository
        extends JpaRepository<Student, Long>,
                JpaSpecificationExecutor<Student> {

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByEmail(String email);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByStudentNumberAndIdNot(
        String studentNumber,
        Long id
    );
}