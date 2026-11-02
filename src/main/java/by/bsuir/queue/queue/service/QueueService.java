package by.bsuir.queue.queue.service;

import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.lab.repository.LabPassRepository;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueueService {

    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final LabPassRepository labPassRepository;

    public QueueService(BookingRepository bookingRepository, SessionRepository sessionRepository, LabPassRepository labPassRepository) {
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.labPassRepository = labPassRepository;
    }

    // Пересборка очереди и назначение текущего
    @Transactional
    public void advanceTurn(Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        List<Booking> queue = buildQueue(session);

        if (queue.isEmpty()) {
            session.setStatus("FINISHED");
            session.setCurrentBooking(null);
            session.setCurrentTurnStartedAt(null);
        } else {
            Booking next = queue.get(0);
            session.setCurrentBooking(next);
            session.setCurrentTurnStartedAt(LocalDateTime.now());
        }
        sessionRepository.save(session);
    }

    // Чистая функция построения очереди
    public List<Booking> buildQueue(Session session) {
        List<Booking> allBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getSession().getId().equals(session.getId()))
                .collect(Collectors.toList());

        // Оставляем только тех, кто еще не сдал всё, что хотел
        List<Booking> active = allBookings.stream()
                .filter(b -> b.getPassesDone() < b.getWantedCount())
                .collect(Collectors.toList());

        List<Booking> deadliners = new ArrayList<>();
        List<Booking> debtors = new ArrayList<>();
        List<Booking> fasts = new ArrayList<>();

        for (Booking b : active) {
            int p = labPassRepository.countByUserIdAndSubjectId(b.getUser().getId(), session.getSubject().getId());
            int k = session.getLabNumber();

            if (p == k - 1) deadliners.add(b);
            else if (p < k - 1) debtors.add(b);
            else fasts.add(b);
        }

        // Сортируем внутри категорий по bookedAt (Вариант X)
        Comparator<Booking> byBookedAt = Comparator.comparing(Booking::getBookedAt);
        deadliners.sort(byBookedAt);
        debtors.sort(byBookedAt);
        fasts.sort(byBookedAt);

        return interleave(deadliners, debtors, fasts);
    }

    // Логика чередования: ДЛ, Д, ДЛ, Д... -> хвост Д -> Быстрые
    private List<Booking> interleave(List<Booking> deadliners, List<Booking> debtors, List<Booking> fasts) {
        List<Booking> result = new ArrayList<>();
        int i = 0, j = 0;
        boolean isDeadlinerTurn = true;

        while (i < deadliners.size() || j < debtors.size()) {
            if (isDeadlinerTurn && i < deadliners.size()) {
                result.add(deadliners.get(i++));
                isDeadlinerTurn = false;
            } else if (!isDeadlinerTurn && j < debtors.size()) {
                result.add(debtors.get(j++));
                isDeadlinerTurn = true;
            } else if (i < deadliners.size()) {
                result.add(deadliners.get(i++)); // Должники кончились
            } else {
                result.add(debtors.get(j++)); // Дедлайнщики кончились
            }
        }
        result.addAll(fasts);
        return result;
    }
}