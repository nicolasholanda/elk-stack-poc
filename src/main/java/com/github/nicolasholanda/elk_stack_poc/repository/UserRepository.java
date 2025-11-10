package com.github.nicolasholanda.elk_stack_poc.repository;

import com.github.nicolasholanda.elk_stack_poc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

