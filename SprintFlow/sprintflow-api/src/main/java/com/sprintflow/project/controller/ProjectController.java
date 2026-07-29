package com.sprintflow.project.controller;

import com.sprintflow.common.response.PageResponse;
import com.sprintflow.project.dto.ProjectRequest;
import com.sprintflow.project.dto.ProjectResponse;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    PageResponse<ProjectResponse> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        String property = switch (sort) {
            case "title", "startDate", "endDate", "status", "createdAt" -> sort;
            default -> "updatedAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return projectService.list(search, status,
                PageRequest.of(page, Math.min(size, 100), Sort.by(dir, property)));
    }

    @GetMapping("/{id}")
    ProjectResponse get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        projectService.delete(id);
    }
}
