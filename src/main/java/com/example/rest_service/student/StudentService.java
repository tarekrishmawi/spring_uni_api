package com.example.rest_service.student;

import com.example.rest_service.common.PageResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> findAll(
            Pageable pageable,
            String search,
            String status) {

        Specification<Student> specification = (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Search student number, first name, last name, or email.
            if (search != null && !search.isBlank()) {

                String pattern = "%" + search.trim().toLowerCase() + "%";

                Predicate studentNumber = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("studentNumber")),
                        pattern);

                Predicate firstName = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("firstName")),
                        pattern);

                Predicate lastName = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("lastName")),
                        pattern);

                Predicate email = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("email")),
                        pattern);

                predicates.add(
                        criteriaBuilder.or(
                                studentNumber,
                                firstName,
                                lastName,
                                email));
            }

            // Filter by status, if supplied.
            if (status != null && !status.isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.upper(
                                        root.get("status")),
                                status.trim().toUpperCase()));
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0]));
        };

        Page<Student> studentPage = repository.findAll(specification, pageable);

        return new PageResponse<>(
                studentPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return toResponse(getStudent(id));
    }

    public StudentResponse create(StudentRequest request) {

        String email = normalizeEmail(request.email());
        String number = request.studentNumber().trim();

        if (repository.existsByStudentNumber(number)) {
            throw conflict("Student number already exists");
        }

        if (repository.existsByEmail(email)) {
            throw conflict("Email already exists");
        }

        Student student = new Student();

        applyRequest(
                student,
                request,
                email,
                number);

        return toResponse(repository.save(student));
    }

    public StudentResponse update(
            Long id,
            StudentRequest request) {

        Student student = getStudent(id);

        String email = normalizeEmail(request.email());
        String number = request.studentNumber().trim();

        if (repository.existsByStudentNumberAndIdNot(number, id)) {
            throw conflict("Student number already exists");
        }

        if (repository.existsByEmailAndIdNot(email, id)) {
            throw conflict("Email already exists");
        }

        applyRequest(
                student,
                request,
                email,
                number);

        return toResponse(repository.save(student));
    }

    public void delete(Long id) {

        Student student = getStudent(id);

        repository.delete(student);
    }

    private Student getStudent(Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"));
    }

    private void applyRequest(
            Student student,
            StudentRequest request,
            String email,
            String number) {

        student.setStudentNumber(number);

        student.setFirstName(
                request.firstName().trim());

        student.setLastName(
                request.lastName().trim());

        student.setEmail(email);

        student.setDateOfBirth(
                request.dateOfBirth());

        student.setPhone(
                request.phone() == null
                        ? null
                        : request.phone().trim());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private StudentResponse toResponse(Student student) {

        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getDateOfBirth(),
                student.getPhone(),
                student.getStatus(),
                student.getCreatedAt(),
                student.getUpdatedAt());
    }

    private ResponseStatusException conflict(String message) {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }
}