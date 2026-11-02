package by.bsuir.queue.booking.repository;

import by.bsuir.queue.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}