package com.sprintflow.user.repository;

import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:search = '' or lower(u.name) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
              and (:enabled is null or u.accountEnabled = :enabled)
            """)
    Page<User> search(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
