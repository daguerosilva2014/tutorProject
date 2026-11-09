package com.academia.repository;

import com.academia.model.ClassPackage;
import com.academia.model.Money;
import com.academia.model.Student;
import com.academia.service.ClassPackageService;
import com.academia.service.StudentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ClassPackageConcurrencyTest {
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ClassPackageService packageService;
    @Autowired StudentService studentService;

    @Test
    void staleVersionCannotCommitAnotherPaymentAmount() {
        Student student = studentService.create("Concurrent Student", null, null);
        ClassPackage classPackage = packageService.create(student.id(), 4,
                new Money(new BigDecimal("100.00")), LocalDate.of(2026, 11, 9));

        EntityManager first = entityManagerFactory.createEntityManager();
        EntityManager second = entityManagerFactory.createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            ClassPackage firstCopy = first.find(ClassPackage.class, classPackage.id());
            ClassPackage secondCopy = second.find(ClassPackage.class, classPackage.id());

            firstCopy.registerPayment(new Money(new BigDecimal("70.00")));
            secondCopy.registerPayment(new Money(new BigDecimal("70.00")));

            first.getTransaction().commit();
            assertThrows(RuntimeException.class, () -> second.getTransaction().commit());
            assertEquals(new BigDecimal("70.00"), packageService.findById(classPackage.id()).paidAmount().amount());
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close();
            second.close();
        }
    }
}
