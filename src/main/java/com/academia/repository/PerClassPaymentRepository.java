package com.academia.repository;

import com.academia.model.PerClassPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PerClassPaymentRepository extends JpaRepository<PerClassPayment, UUID> {
    List<PerClassPayment> findByParticipationIdOrderByPaidAtAscIdAsc(UUID participationId);
}
