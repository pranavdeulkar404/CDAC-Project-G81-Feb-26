package com.sprintflow.controller;

import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository repository;
    private final UserRepository userRepository;

    public ProjectController(ProjectRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ProjectResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        Project project = new Project();
        apply(project, request);
        return toResponse(repository.save(project));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        Project project = find(id);
        apply(project, request);
        return toResponse(repository.save(project));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Project find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator user not found"));
    }

    private void apply(Project project, ProjectRequest request) {
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setStatus(request.status());
        project.setCreatedBy(findUser(request.createdById()));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                project.getStartDate(), project.getStatus(), project.getCreatedBy().getId(),
                project.getCreatedBy().getName());
    }

    public record ProjectRequest(
            @NotBlank String title, String description, LocalDate startDate,
            @NotBlank String status, @NotNull Long createdById) {}

    public record ProjectResponse(
            Long id, String title, String description, LocalDate startDate, String status,
            Long createdById, String createdByName) {}
}
