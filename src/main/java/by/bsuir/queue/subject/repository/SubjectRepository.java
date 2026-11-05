package by.bsuir.queue.subject.repository;

import by.bsuir.queue.subject.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Subject findByCode(String code);
}