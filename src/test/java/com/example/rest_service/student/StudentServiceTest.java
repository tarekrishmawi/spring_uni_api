package com.example.rest_service.student;

import com.example.rest_service.common.PageResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @InjectMocks
    private StudentService service;

    private StudentRequest request;

    @BeforeEach
    void setUp() {
        request = new StudentRequest(
            "20260001",
            "Ahmad",
            "Khalil",
            "AHMAD@example.com",
            null,
            "0599000000"
        );
    }

    @Test
    void createStudentSuccessfully() {

        when(repository.existsByStudentNumber("20260001"))
            .thenReturn(false);

        when(repository.existsByEmail("ahmad@example.com"))
            .thenReturn(false);

        when(repository.save(any(Student.class)))
            .thenAnswer(invocation -> {
                Student student = invocation.getArgument(0);
                return student;
            });

        StudentResponse result = service.create(request);

        assertEquals(
            "20260001",
            result.studentNumber()
        );

        assertEquals(
            "ahmad@example.com",
            result.email()
        );

        assertEquals(
            "Ahmad",
            result.firstName()
        );

        verify(repository).save(any(Student.class));
    }

    @Test
    void rejectDuplicateStudentNumber() {

        when(repository.existsByStudentNumber("20260001"))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(request)
        );

        assertEquals(
            409,
            exception.getStatusCode().value()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void rejectDuplicateEmail() {

        when(repository.existsByStudentNumber("20260001"))
            .thenReturn(false);

        when(repository.existsByEmail("ahmad@example.com"))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.create(request)
        );

        assertEquals(
            409,
            exception.getStatusCode().value()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void returnNotFoundForMissingStudent() {

        when(repository.findById(999L))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.findById(999L)
        );

        assertEquals(
            404,
            exception.getStatusCode().value()
        );
    }

    @Test
    void listStudentsWithPagination() {

        Student student = new Student();

        student.setStudentNumber("S001");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@example.com");

        Pageable pageable = PageRequest.of(0, 10);

        Page<Student> studentPage =
            new PageImpl<>(
                List.of(student),
                pageable,
                1
            );

        when(repository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<Student>>any(),
            any(Pageable.class)
        )).thenReturn(studentPage);

        PageResponse<StudentResponse> result =
            service.findAll(
                pageable,
                null,
                null
            );

        assertEquals(
            1,
            result.content().size()
        );

        assertEquals(
            0,
            result.page()
        );

        assertEquals(
            10,
            result.size()
        );

        assertEquals(
            1,
            result.totalElements()
        );

        assertEquals(
            1,
            result.totalPages()
        );

        verify(repository).findAll(
            org.mockito.ArgumentMatchers
                .<Specification<Student>>any(),
            eq(pageable)
        );
    }

    @Test
    void searchStudentsReturnsMatchingResults() {

        Student student = new Student();

        student.setStudentNumber("20260001");
        student.setFirstName("Ahmad");
        student.setLastName("Khalil");
        student.setEmail("ahmad@example.com");
        student.setStatus("ACTIVE");

        Pageable pageable = PageRequest.of(0, 10);

        Page<Student> studentPage =
            new PageImpl<>(
                List.of(student),
                pageable,
                1
            );

        when(repository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<Student>>any(),
            any(Pageable.class)
        )).thenReturn(studentPage);

        PageResponse<StudentResponse> result =
            service.findAll(
                pageable,
                "ahmad",
                "ACTIVE"
            );

        assertEquals(
            1,
            result.totalElements()
        );

        assertEquals(
            "Ahmad",
            result.content()
                .get(0)
                .firstName()
        );

        assertEquals(
            "ACTIVE",
            result.content()
                .get(0)
                .status()
        );
    }

    @Test
    void searchWithoutFiltersReturnsEmptyResults() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Student> emptyPage =
            new PageImpl<>(
                List.of(),
                pageable,
                0
            );

        when(repository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<Student>>any(),
            any(Pageable.class)
        )).thenReturn(emptyPage);

        PageResponse<StudentResponse> result =
            service.findAll(
                pageable,
                null,
                null
            );

        assertEquals(
            0,
            result.totalElements()
        );

        assertTrue(
            result.content().isEmpty()
        );
    }
}