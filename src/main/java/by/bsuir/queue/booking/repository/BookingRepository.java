package by.bsuir.queue.booking.repository;

import by.bsuir.queue.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

    @Query("select b from Booking b join fetch b.user where b.session.id = :sessionId")
    List<Booking> findBySessionIdWithUser(@Param("sessionId") Long sessionId);

    @Query("select b.session.id from Booking b where b.user.studentId = :studentId")
    List<Long> findSessionIdsByStudentId(@Param("studentId") String studentId);
}
