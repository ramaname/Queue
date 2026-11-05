package by.bsuir.queue.subject.controller;

import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.subject.repository.SubjectRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final SubjectRepository subjectRepository;

    public HealthController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/subjects")
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }
}