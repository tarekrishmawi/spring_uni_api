package com.example.rest_service.student;

import com.example.rest_service.common.PageResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "studentNumber",
        "firstName",
        "lastName",
        "email",
        "status",
        "createdAt"
    );

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<StudentResponse> getAll(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {

        if (page < 0 || size < 1) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page must be >= 0 and size must be >= 1"
            );
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported sort field: " + sortBy
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection =
                Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Direction must be 'asc' or 'desc'"
            );
        }

        Pageable pageable = PageRequest.of(
            page,
            Math.min(size, 100),
            Sort.by(sortDirection, sortBy)
        );

        return service.findAll(pageable, search, status);
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(
        @Valid @RequestBody StudentRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StudentResponse update(
        @PathVariable Long id,
        @Valid @RequestBody StudentRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}