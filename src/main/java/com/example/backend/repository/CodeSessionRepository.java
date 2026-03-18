package com.example.backend.repository;

import com.example.backend.entity.CodeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CodeSessionRepository extends JpaRepository<CodeSession, UUID> {
}
