package com.sprintflow.comment.controller;

import com.sprintflow.comment.dto.CommentRequest;
import com.sprintflow.comment.dto.CommentResponse;
import com.sprintflow.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tasks/{taskId}/comments")
    List<CommentResponse> taskComments(@PathVariable Long taskId) {
        return commentService.taskComments(taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentResponse addTaskComment(@PathVariable Long taskId, @Valid @RequestBody CommentRequest request) {
        return commentService.addTaskComment(taskId, request);
    }

    @GetMapping("/bugs/{bugId}/comments")
    List<CommentResponse> bugComments(@PathVariable Long bugId) {
        return commentService.bugComments(bugId);
    }

    @PostMapping("/bugs/{bugId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentResponse addBugComment(@PathVariable Long bugId, @Valid @RequestBody CommentRequest request) {
        return commentService.addBugComment(bugId, request);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        commentService.delete(id);
    }
}
