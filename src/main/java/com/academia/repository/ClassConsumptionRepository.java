package com.academia.repository;

import com.academia.model.ClassConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassConsumptionRepository extends JpaRepository<ClassConsumption, UUID> {
}
