package com.academia.repository;

import com.academia.model.UserStatus;
import com.academia.model.UserRole;
import com.academia.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(UserRole role);

    @Query("""
        select u from User u
        where (:role is null or u.role = :role)
          and (:status is null or u.status = :status)
        """)
    List<User> search(UserRole role, UserStatus status, Sort sort);
}
