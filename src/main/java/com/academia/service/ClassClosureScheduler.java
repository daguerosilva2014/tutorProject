package com.academia.service;

import com.academia.exception.NoPackageBalanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClassClosureScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClassClosureScheduler.class);
    private final ClassClosureService service;

    public ClassClosureScheduler(ClassClosureService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void closeDueClasses() {
        for (var classId : service.findDueIds()) {
            try {
                service.close(classId);
            } catch (NoPackageBalanceException | OptimisticLockingFailureException exception) {
                log.warn("Could not close class {}: {}", classId, exception.getClass().getSimpleName());
            } catch (RuntimeException exception) {
                log.error("Could not close class {}", classId, exception);
            }
        }
    }
}
