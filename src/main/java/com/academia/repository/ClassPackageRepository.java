package com.academia.repository;

import com.academia.model.ClassPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassPackageRepository extends JpaRepository<ClassPackage, UUID> {
    List<ClassPackage> findByStudentIdOrderByPurchasedAtAscIdAsc(UUID studentId);
}
