package com.sprintflow.bug.controller;

import com.sprintflow.bug.dto.BugRequest;
import com.sprintflow.bug.dto.BugResponse;
import com.sprintflow.bug.dto.BugStatusRequest;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import com.sprintflow.bug.service.BugService;
import com.sprintflow.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bugs")
public class BugController {

    private final BugService bugService;

    public BugController(BugService bugService) {
        this.bugService = bugService;
    }

    @GetMapping
    PageResponse<BugResponse> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) BugStatus status,
            @RequestParam(required = false) BugSeverity severity,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        String property = switch (sort) {
            case "title", "severity", "status", "createdAt" -> sort;
            default -> "updatedAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return bugService.list(projectId, assigneeId, status, severity, search,
                PageRequest.of(page, Math.min(size, 100), Sort.by(dir, property)));
    }

    @GetMapping("/{id}")
    BugResponse get(@PathVariable Long id) {
        return bugService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BugResponse create(@Valid @RequestBody BugRequest request) {
        return bugService.create(request);
    }

    @PutMapping("/{id}")
    BugResponse update(@PathVariable Long id, @Valid @RequestBody BugRequest request) {
        return bugService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    BugResponse updateStatus(@PathVariable Long id, @Valid @RequestBody BugStatusRequest request) {
        return bugService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        bugService.delete(id);
    }
}
