package com.sprintflow.controller;

import com.sprintflow.entity.Profile;
import com.sprintflow.entity.User;
import com.sprintflow.repository.ProfileRepository;
import com.sprintflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileRepository repository;
    private final UserRepository userRepository;

    public ProfileController(ProfileRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ProfileResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProfileResponse getById(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(@Valid @RequestBody ProfileRequest request) {
        Profile profile = new Profile();
        apply(profile, request);
        return toResponse(repository.save(profile));
    }

    @PutMapping("/{id}")
    public ProfileResponse update(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        Profile profile = find(id);
        apply(profile, request);
        return toResponse(repository.save(profile));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(find(id));
    }

    private Profile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void apply(Profile profile, ProfileRequest request) {
        profile.setUser(findUser(request.userId()));
        profile.setPhone(request.phone());
        profile.setDesignation(request.designation());
        profile.setBio(request.bio());
    }

    private ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getUser().getId(), profile.getUser().getName(),
                profile.getPhone(), profile.getDesignation(), profile.getBio());
    }

    public record ProfileRequest(@NotNull Long userId, String phone, String designation, String bio) {}
    public record ProfileResponse(Long id, Long userId, String userName, String phone, String designation, String bio) {}
}
