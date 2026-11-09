package com.academia.service;

import com.academia.exception.InvalidPaymentException;
import com.academia.exception.PaymentExceedsBalanceException;
import com.academia.model.ClassPackage;
import com.academia.model.Money;
import com.academia.model.Payment;
import com.academia.model.PaymentMethod;
import com.academia.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final ClassPackageService packageService;
    private final UserService userService;

    public PaymentService(PaymentRepository repository, ClassPackageService packageService, UserService userService) {
        this.repository = repository;
        this.packageService = packageService;
        this.userService = userService;
    }

    @Transactional
    public Payment create(String username, UUID packageId, Money amount, LocalDate paidAt,
                          PaymentMethod method, String notes) {
        ClassPackage classPackage = packageService.findById(packageId);
        if (amount.amount().signum() <= 0) {
            throw new InvalidPaymentException();
        }
        if (amount.compareTo(classPackage.outstandingAmount()) > 0) {
            throw new PaymentExceedsBalanceException();
        }
        classPackage.registerPayment(amount);
        return repository.save(Payment.create(classPackage, amount, paidAt, method, notes,
                userService.findByUsername(username)));
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll(UUID packageId) {
        packageService.findById(packageId);
        return repository.findByClassPackageIdOrderByPaidAtAscIdAsc(packageId);
    }
}
