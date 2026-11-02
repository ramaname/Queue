package by.bsuir.queue.session.repository;

import by.bsuir.queue.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
}