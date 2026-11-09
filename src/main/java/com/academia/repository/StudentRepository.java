package com.academia.repository;

import com.academia.model.Student;
import com.academia.model.StudentStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    @Query("""
            select s from Student s
            where (:status is null or s.status = :status)
              and (:search is null or lower(s.name) like lower(concat('%', :search, '%')))
            """)
    List<Student> search(StudentStatus status, String search, Sort sort);
}
