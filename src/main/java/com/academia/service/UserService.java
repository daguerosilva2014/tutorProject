package com.academia.service;

import com.academia.exception.InvalidPasswordException;
import com.academia.exception.UserNotFoundException;
import com.academia.exception.UsernameAlreadyExistsException;
import com.academia.model.UserStatus;
import com.academia.model.UserRole;
import com.academia.model.User;
import com.academia.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String name, String username, String rawPassword, UserRole role) {
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new InvalidPasswordException();
        }

        User newUser = User.create(name, username, passwordEncoder.encode(rawPassword), role);
        return repository.save(newUser);
    }

    @Transactional(readOnly = true)
    public List<User> findAll(UserRole role, UserStatus status) {
        return repository.search(role, status, Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return repository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username).orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public User update(UUID id, String name, UserRole role, boolean isActive) {
        User user = findById(id);
        user.update(name, role, isActive ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        return repository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean hasAdmin() {
        return repository.existsByRole(UserRole.ADMIN);
    }

}
