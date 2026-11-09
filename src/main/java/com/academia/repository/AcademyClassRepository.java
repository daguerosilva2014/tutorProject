package com.academia.repository;

import com.academia.model.AcademyClass;
import com.academia.model.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AcademyClassRepository extends JpaRepository<AcademyClass, UUID> {
    @Query("""
            select c from AcademyClass c
            where c.status = 'SCHEDULED'
              and (c.date < :today or (c.date = :today and c.endTime <= :currentTime))
            order by c.date, c.endTime, c.id
            """)
    List<AcademyClass> findDue(LocalDate today, LocalTime currentTime);

    @Query("""
            select c from AcademyClass c
            where (:fromDate is null or c.date >= :fromDate)
              and (:toDate is null or c.date <= :toDate)
              and (:teacherId is null or c.teacher.id = :teacherId)
              and (:status is null or c.status = :status)
            order by c.date, c.startTime, c.id
            """)
    List<AcademyClass> search(LocalDate fromDate, LocalDate toDate, UUID teacherId, ClassStatus status);
}
