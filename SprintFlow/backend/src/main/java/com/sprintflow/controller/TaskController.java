package com.sprintflow.controller;

import com.sprintflow.entity.Project;
import com.sprintflow.entity.Task;
import com.sprintflow.entity.User;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.TaskRepository;
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
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskRepository repository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository repository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<TaskResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        return toResponse(repository.save(task));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        Task task = find(id);
        apply(task, request);
        return toResponse(repository.save(task));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Task find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned user not found"));
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
        task.setProject(findProject(request.projectId()));
        task.setAssignedTo(findUser(request.assignedToId()));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getPriority(),
                task.getStatus(), task.getDueDate(), task.getProject().getId(), task.getProject().getTitle(),
                task.getAssignedTo().getId(), task.getAssignedTo().getName());
    }

    public record TaskRequest(
            @NotBlank String title, String description, @NotBlank String priority,
            @NotBlank String status, LocalDate dueDate,
            @NotNull Long projectId, @NotNull Long assignedToId) {}

    public record TaskResponse(
            Long id, String title, String description, String priority, String status, LocalDate dueDate,
            Long projectId, String projectTitle, Long assignedToId, String assignedToName) {}
}
