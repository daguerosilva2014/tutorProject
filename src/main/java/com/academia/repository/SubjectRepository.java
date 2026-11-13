package com.academia.repository;

import com.academia.model.Subject;
import com.academia.model.SubjectStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    @Query("""
            select s from Subject s
            where (:status is null or s.status = :status)
              and (:search = '' or lower(s.name) like lower(concat('%', :search, '%')))
            """)
    List<Subject> search(SubjectStatus status, String search, Sort sort);
}
