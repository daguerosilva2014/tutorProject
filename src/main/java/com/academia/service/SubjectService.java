package com.academia.service;

import com.academia.exception.SubjectNotFoundException;
import com.academia.model.Subject;
import com.academia.model.SubjectStatus;
import com.academia.repository.SubjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubjectService {
    private final SubjectRepository repository;

    public SubjectService(SubjectRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Subject create(String name, String description) {
        return repository.save(Subject.create(name, description));
    }

    @Transactional(readOnly = true)
    public List<Subject> findAll(SubjectStatus status, String search) {
        String term = search == null || search.isBlank() ? "" : search.trim();
        return repository.search(status, term, Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public Subject findById(UUID id) {
        return repository.findById(id).orElseThrow(SubjectNotFoundException::new);
    }

    @Transactional
    public Subject update(UUID id, String name, String description) {
        Subject subject = findById(id);
        subject.update(name, description);
        return subject;
    }

    @Transactional
    public Subject changeStatus(UUID id, SubjectStatus status) {
        Subject subject = findById(id);
        subject.changeStatus(status);
        return subject;
    }
}
