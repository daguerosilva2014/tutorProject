package com.academia.service;

import com.academia.exception.ClassParticipationNotFoundException;
import com.academia.exception.InvalidBillingModeException;
import com.academia.exception.InvalidClassStateException;
import com.academia.exception.InvalidPaymentException;
import com.academia.exception.PaymentExceedsBalanceException;
import com.academia.model.BillingMode;
import com.academia.model.ClassParticipation;
import com.academia.model.ClassStatus;
import com.academia.model.Money;
import com.academia.model.PaymentMethod;
import com.academia.model.PerClassPayment;
import com.academia.repository.ClassParticipationRepository;
import com.academia.repository.PerClassPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PerClassPaymentService {
    private final PerClassPaymentRepository repository;
    private final ClassParticipationRepository participationRepository;
    private final AcademyClassService classService;
    private final UserService userService;

    public PerClassPaymentService(PerClassPaymentRepository repository,
                                  ClassParticipationRepository participationRepository,
                                  AcademyClassService classService, UserService userService) {
        this.repository = repository;
        this.participationRepository = participationRepository;
        this.classService = classService;
        this.userService = userService;
    }

    @Transactional
    public PerClassPayment create(String username, UUID participationId, Money amount, LocalDate paidAt,
                                  PaymentMethod method, String notes) {
        ClassParticipation participation = findAuthorized(username, participationId);
        if (participation.billingMode() != BillingMode.PER_CLASS) throw new InvalidBillingModeException();
        if (participation.academyClass().status() != ClassStatus.FINALIZED) throw new InvalidClassStateException();
        if (amount.amount().signum() <= 0) throw new InvalidPaymentException();
        if (amount.compareTo(participation.outstandingAmount()) > 0) throw new PaymentExceedsBalanceException();
        participation.registerPayment(amount);
        return repository.save(PerClassPayment.create(participation, amount, paidAt, method, notes,
                userService.findByUsername(username)));
    }

    @Transactional(readOnly = true)
    public List<PerClassPayment> findAll(String username, UUID participationId) {
        ClassParticipation participation = findAuthorized(username, participationId);
        if (participation.billingMode() != BillingMode.PER_CLASS) throw new InvalidBillingModeException();
        return repository.findByParticipationIdOrderByPaidAtAscIdAsc(participationId);
    }

    private ClassParticipation findAuthorized(String username, UUID id) {
        ClassParticipation participation = participationRepository.findById(id)
                .orElseThrow(ClassParticipationNotFoundException::new);
        classService.findById(username, participation.academyClass().id());
        return participation;
    }
}
