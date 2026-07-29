package com.sprintflow.task.controller;

import com.sprintflow.common.response.PageResponse;
import com.sprintflow.task.dto.TaskRequest;
import com.sprintflow.task.dto.TaskResponse;
import com.sprintflow.task.dto.TaskStatusRequest;
import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import com.sprintflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    PageResponse<TaskResponse> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        String property = switch (sort) {
            case "title", "priority", "status", "dueDate", "createdAt" -> sort;
            default -> "updatedAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return taskService.list(projectId, assigneeId, status, priority, search,
                PageRequest.of(page, Math.min(size, 100), Sort.by(dir, property)));
    }

    @GetMapping("/{id}")
    TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
