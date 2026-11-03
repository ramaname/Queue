package by.bsuir.queue.session.repository;
import by.bsuir.queue.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByStatusAndUnlockAtBefore(String status, LocalDateTime now);
    List<Session> findByStatus(String status);
}