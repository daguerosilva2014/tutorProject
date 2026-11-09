package com.academia.repository;

import com.academia.model.ClassParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassParticipationRepository extends JpaRepository<ClassParticipation, UUID> {
    List<ClassParticipation> findByAcademyClassIdOrderByStudentNameAsc(UUID classId);

    boolean existsByAcademyClassIdAndStudentId(UUID classId, UUID studentId);
}
