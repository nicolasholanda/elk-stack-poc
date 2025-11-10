package com.github.nicolasholanda.elk_stack_poc.service;

import com.github.nicolasholanda.elk_stack_poc.model.User;
import com.github.nicolasholanda.elk_stack_poc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        log.info("Creating new user with email: {}", user.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        try {
            User savedUser = userRepository.save(user);
            log.info("User created successfully with id: {}", savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            log.error("Error creating user with email: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        return userRepository.findByEmail(email);
    }

    public User updateUser(Long id, User userDetails) {
        log.info("Updating user with id: {}", id);

        return userRepository.findById(id)
                .map(user -> {
                    log.debug("User found, updating details for id: {}", id);
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    user.setPhone(userDetails.getPhone());
                    user.setUpdatedAt(LocalDateTime.now());
                    User updated = userRepository.save(user);
                    log.info("User updated successfully with id: {}", id);
                    return updated;
                })
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new RuntimeException("User not found with id: " + id);
                });
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            log.info("User deleted successfully with id: {}", id);
        } else {
            log.warn("User not found for deletion with id: {}", id);
            throw new RuntimeException("User not found with id: " + id);
        }
    }
}

