package com.academia.service;

import com.academia.exception.ClassPackageNotFoundException;
import com.academia.exception.NoPackageBalanceException;
import com.academia.model.ClassPackage;
import com.academia.model.Money;
import com.academia.model.PackagePaymentStatus;
import com.academia.model.PackageUsageStatus;
import com.academia.repository.ClassPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ClassPackageService {
    private final ClassPackageRepository repository;
    private final StudentService studentService;

    public ClassPackageService(ClassPackageRepository repository, StudentService studentService) {
        this.repository = repository;
        this.studentService = studentService;
    }

    @Transactional
    public ClassPackage create(UUID studentId, int totalClasses, Money price, LocalDate purchasedAt) {
        return repository.save(ClassPackage.create(studentService.findById(studentId), totalClasses, price, purchasedAt));
    }

    @Transactional(readOnly = true)
    public List<ClassPackage> findAll(UUID studentId, PackageUsageStatus usageStatus,
                                      PackagePaymentStatus paymentStatus) {
        studentService.findById(studentId);
        return repository.findByStudentIdOrderByPurchasedAtAscIdAsc(studentId).stream()
                .filter(classPackage -> usageStatus == null || classPackage.usageStatus() == usageStatus)
                .filter(classPackage -> paymentStatus == null || classPackage.paymentStatus() == paymentStatus)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassPackage findById(UUID id) {
        return repository.findById(id).orElseThrow(ClassPackageNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableClasses(UUID studentId) {
        return repository.findByStudentIdOrderByPurchasedAtAscIdAsc(studentId).stream()
                .anyMatch(classPackage -> classPackage.remainingClasses() > 0);
    }

    @Transactional
    public ClassPackage findOldestAvailable(UUID studentId) {
        return repository.findByStudentIdOrderByPurchasedAtAscIdAsc(studentId).stream()
                .filter(classPackage -> classPackage.remainingClasses() > 0)
                .findFirst()
                .orElseThrow(NoPackageBalanceException::new);
    }
}
