package by.bsuir.queue.session.repository;

import by.bsuir.queue.session.model.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByStatusAndUnlockAtBefore(String status, LocalDateTime now);

    List<Session> findByStatus(String status);

    /** Блокировка строки сессии: сдача, пропуск, запись и планировщик не наступают друг другу на пятки. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.id = :id")
    Optional<Session> findByIdForUpdate(@Param("id") Long id);
}
