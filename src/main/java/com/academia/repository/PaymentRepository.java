package com.academia.repository;

import com.academia.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByClassPackageIdOrderByPaidAtAscIdAsc(UUID packageId);
}
