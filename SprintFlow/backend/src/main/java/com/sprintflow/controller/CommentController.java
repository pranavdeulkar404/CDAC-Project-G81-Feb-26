package com.sprintflow.controller;

import com.sprintflow.entity.Bug;
import com.sprintflow.entity.Comment;
import com.sprintflow.entity.Task;
import com.sprintflow.entity.User;
import com.sprintflow.repository.BugRepository;
import com.sprintflow.repository.CommentRepository;
import com.sprintflow.repository.TaskRepository;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentRepository repository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final BugRepository bugRepository;

    public CommentController(CommentRepository repository, UserRepository userRepository,
                             TaskRepository taskRepository, BugRepository bugRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.bugRepository = bugRepository;
    }

    @GetMapping
    public List<CommentResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CommentResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@Valid @RequestBody CommentRequest request) {
        Comment comment = new Comment();
        apply(comment, request);
        return toResponse(repository.save(comment));
    }

    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        Comment comment = find(id);
        apply(comment, request);
        return toResponse(repository.save(comment));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Comment find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Bug findBug(Long id) {
        return bugRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug not found"));
    }

    private void apply(Comment comment, CommentRequest request) {
        boolean hasTask = request.taskId() != null;
        boolean hasBug = request.bugId() != null;
        if (hasTask == hasBug) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose exactly one comment target: task or bug");
        }

        comment.setMessage(request.message());
        comment.setCreatedAt(request.createdAt() == null ? LocalDateTime.now() : request.createdAt());
        comment.setUser(findUser(request.userId()));
        comment.setTask(hasTask ? findTask(request.taskId()) : null);
        comment.setBug(hasBug ? findBug(request.bugId()) : null);
    }

    private CommentResponse toResponse(Comment comment) {
        String targetType = comment.getTask() != null ? "TASK" : "BUG";
        Long targetId = comment.getTask() != null ? comment.getTask().getId() : comment.getBug().getId();
        String targetTitle = comment.getTask() != null ? comment.getTask().getTitle() : comment.getBug().getTitle();
        return new CommentResponse(comment.getId(), comment.getMessage(), comment.getCreatedAt(),
                comment.getUser().getId(), comment.getUser().getName(), targetType, targetId, targetTitle,
                comment.getTask() == null ? null : comment.getTask().getId(),
                comment.getBug() == null ? null : comment.getBug().getId());
    }

    public record CommentRequest(
            @NotBlank String message, LocalDateTime createdAt, @NotNull Long userId,
            Long taskId, Long bugId) {}

    public record CommentResponse(
            Long id, String message, LocalDateTime createdAt, Long userId, String userName,
            String targetType, Long targetId, String targetTitle, Long taskId, Long bugId) {}
}
