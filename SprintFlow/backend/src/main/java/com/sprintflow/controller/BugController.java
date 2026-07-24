package com.sprintflow.controller;

import com.sprintflow.entity.Bug;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.BugRepository;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
public class BugController {
    private final BugRepository repository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public BugController(BugRepository repository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<BugResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BugResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BugResponse create(@Valid @RequestBody BugRequest request) {
        Bug bug = new Bug();
        apply(bug, request);
        return toResponse(repository.save(bug));
    }

    @PutMapping("/{id}")
    public BugResponse update(@PathVariable Long id, @Valid @RequestBody BugRequest request) {
        Bug bug = find(id);
        apply(bug, request);
        return toResponse(repository.save(bug));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Bug find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug not found"));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned user not found"));
    }

    private void apply(Bug bug, BugRequest request) {
        bug.setTitle(request.title());
        bug.setDescription(request.description());
        bug.setSeverity(request.severity());
        bug.setStatus(request.status());
        bug.setProject(findProject(request.projectId()));
        bug.setAssignedTo(findUser(request.assignedToId()));
    }

    private BugResponse toResponse(Bug bug) {
        return new BugResponse(bug.getId(), bug.getTitle(), bug.getDescription(), bug.getSeverity(),
                bug.getStatus(), bug.getProject().getId(), bug.getProject().getTitle(),
                bug.getAssignedTo().getId(), bug.getAssignedTo().getName());
    }

    public record BugRequest(
            @NotBlank String title, String description, @NotBlank String severity,
            @NotBlank String status, @NotNull Long projectId, @NotNull Long assignedToId) {}

    public record BugResponse(
            Long id, String title, String description, String severity, String status,
            Long projectId, String projectTitle, Long assignedToId, String assignedToName) {}
}
