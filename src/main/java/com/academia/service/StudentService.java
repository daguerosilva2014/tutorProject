package com.academia.service;

import com.academia.exception.StudentNotFoundException;
import com.academia.model.Student;
import com.academia.model.StudentStatus;
import com.academia.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Student create(String name, String contactNumber, String notes) {
        return repository.save(Student.create(name, contactNumber, notes));
    }

    @Transactional(readOnly = true)
    public List<Student> findAll(StudentStatus status, String search) {
        String term = search == null || search.isBlank() ? null : search.trim();
        return repository.search(status, term, Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public Student findById(UUID id) {
        return repository.findById(id).orElseThrow(StudentNotFoundException::new);
    }

    @Transactional
    public Student update(UUID id, String name, String contactNumber, String notes) {
        Student student = findById(id);
        student.update(name, contactNumber, notes);
        return student;
    }

    @Transactional
    public Student changeStatus(UUID id, StudentStatus status) {
        Student student = findById(id);
        student.changeStatus(status);
        return student;
    }
}
