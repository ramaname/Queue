package by.bsuir.queue.session.service;

import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.queue.service.QueueService;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionScheduler {
    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final QueueService queueService;

    public SessionScheduler(SessionRepository sessionRepository, BookingRepository bookingRepository, QueueService queueService) {
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.queueService = queueService;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkOpenSessions() {
        List<Session> toOpen = sessionRepository.findByStatusAndUnlockAtBefore("SCHEDULED", LocalDateTime.now());
        for (Session s : toOpen) {
            s.setStatus("OPEN"); sessionRepository.save(s);
            System.out.println("СИСТЕМА: Сессия " + s.getId() + " открыта!");
        }
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkCloseSessions() {
        for (Session s : sessionRepository.findByStatus("OPEN")) {
            if (LocalDateTime.now().isAfter(s.getUnlockAt().plusMinutes(s.getWindowMinutes()))) {
                s.setStatus("CLOSED"); sessionRepository.save(s);
                System.out.println("СИСТЕМА: Окно сессии " + s.getId() + " закрыто.");
                queueService.advanceTurn(s.getId());
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkTimeout() {
        for (Session s : sessionRepository.findByStatus("CLOSED")) {
            if (s.getCurrentBooking() != null && s.getCurrentTurnStartedAt() != null) {
                if (LocalDateTime.now().isAfter(s.getCurrentTurnStartedAt().plusMinutes(3))) {
                    Booking b = s.getCurrentBooking();
                    System.out.println("СИСТЕМА: Таймаут у " + b.getUser().getFullName() + ". Автоскип.");
                    b.setSkippedAt(LocalDateTime.now()); bookingRepository.save(b);
                    queueService.advanceTurn(s.getId());
                }
            }
        }
    }
}