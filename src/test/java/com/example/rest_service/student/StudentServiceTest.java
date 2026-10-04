package com.example.rest_service.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.rest_service.common.PageResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
                "0599000000");
    }

    @Test
    void createStudentSuccessfully() {
        when(repository.existsByStudentNumber(
                "20260001")).thenReturn(false);

        when(repository.existsByEmail(
                "ahmad@example.com")).thenReturn(false);

        when(repository.save(any(Student.class)))
                .thenAnswer(invocation -> {
                    Student student = invocation.getArgument(0);
                    return student;
                });

        StudentResponse result = service.create(request);

        assertEquals("20260001", result.studentNumber());
        assertEquals("ahmad@example.com", result.email());
        assertEquals("Ahmad", result.firstName());

        verify(repository).save(any(Student.class));
    }

    @Test
    void rejectDuplicateStudentNumber() {
        when(repository.existsByStudentNumber(
                "20260001")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request));

        assertEquals(
                409,
                exception.getStatusCode().value());

        verify(repository, never()).save(any());
    }

    @Test
    void rejectDuplicateEmail() {
        when(repository.existsByStudentNumber(
                "20260001")).thenReturn(false);

        when(repository.existsByEmail(
                "ahmad@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request));

        assertEquals(
                409,
                exception.getStatusCode().value());

        verify(repository, never()).save(any());
    }

    @Test
    void returnNotFoundForMissingStudent() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(999L));

        assertEquals(
                404,
                exception.getStatusCode().value());
    }

    @Test
    void listStudentsWithPagination() {

        Page<Student> emptyPage = new PageImpl<>(
                java.util.List.of(),
                PageRequest.of(0, 10),
                0);

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<StudentResponse> result = service.findAll(PageRequest.of(0, 10), null, null);

        assertEquals(0, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());

        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void searchStudentsReturnsMatchingResults() {

        Student student = new Student();
        student.setStudentNumber("20260001");
        student.setFirstName("Ahmad");
        student.setLastName("Khalil");
        student.setEmail("ahmad@example.com");
        student.setStatus("ACTIVE");

        Page<Student> studentPage = new PageImpl<>(
                java.util.List.of(student),
                PageRequest.of(0, 10),
                1);

        when(repository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Student>>any(),
                any(Pageable.class))).thenReturn(studentPage);

        PageResponse<StudentResponse> result = service.findAll(
                PageRequest.of(0, 10),
                "ahmad",
                "ACTIVE");

        assertEquals(1, result.totalElements());
        assertEquals("Ahmad", result.content().get(0).firstName());
        assertEquals("ACTIVE", result.content().get(0).status());
    }

    @Test
    void searchWithoutFiltersReturnsEmptyResults() {

        Page<Student> emptyPage = new PageImpl<>(
                java.util.List.of(),
                PageRequest.of(0, 10),
                0);

        when(repository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Student>>any(),
                any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<StudentResponse> result = service.findAll(
                PageRequest.of(0, 10),
                null,
                null);

        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
    }
}