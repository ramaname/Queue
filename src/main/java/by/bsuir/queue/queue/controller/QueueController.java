package by.bsuir.queue.queue.controller;

import by.bsuir.queue.queue.dto.QueueItemDto;
import java.util.ArrayList;
import by.bsuir.queue.booking.dto.BookRequest;
import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.lab.model.LabPass;
import by.bsuir.queue.lab.repository.LabPassRepository;
import by.bsuir.queue.queue.service.QueueService;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
public class QueueController {

    private final SessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final LabPassRepository labPassRepository;
    private final QueueService queueService;

    public QueueController(SessionRepository sessionRepository, BookingRepository bookingRepository, UserRepository userRepository, LabPassRepository labPassRepository, QueueService queueService) {
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.labPassRepository = labPassRepository;
        this.queueService = queueService;
    }

    // 1. Занять очередь
    @PostMapping("/book")
    @Transactional
    public String book(@PathVariable Long sessionId, @RequestBody BookRequest request) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByStudentId(studentId).orElseThrow();

        if (!session.getStatus().equals("OPEN")) throw new RuntimeException("Окно бронирования закрыто");

        if (bookingRepository.findAll().stream().anyMatch(b -> b.getSession().getId().equals(sessionId) && b.getUser().getId().equals(user.getId()))) {
            throw new RuntimeException("Вы уже заняли очередь");
        }

        Booking booking = new Booking();
        booking.setSession(session);
        booking.setUser(user);
        booking.setWantedCount(request.getWantedCount());
        booking.setPassesDone(0);
        booking.setVersion(0);
        // booked_at ставится автоматически в БД
        bookingRepository.save(booking);

        return "Вы успешно заняли очередь!";
    }

    // 2. Посмотреть текущую очередь
    @GetMapping("/queue")
    @Transactional(readOnly = true) // Важно, чтобы Hibernate не ругался на Lazy-связи
    public List<QueueItemDto> getQueue(@PathVariable Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        List<Booking> queue = queueService.buildQueue(session);

        List<QueueItemDto> result = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            Booking b = queue.get(i);

            // Считаем категорию для отображения
            int p = labPassRepository.countByUserIdAndSubjectId(b.getUser().getId(), session.getSubject().getId());
            int k = session.getLabNumber();
            String category = (p == k - 1) ? "DEADLINER" : (p < k - 1 ? "DEBTOR" : "FAST");

            // Проверяем, чей сейчас ход
            boolean isCurrent = session.getCurrentBooking() != null && session.getCurrentBooking().getId().equals(b.getId());

            result.add(new QueueItemDto(
                    i + 1, // Позиция в очереди (с 1, а не с 0)
                    b.getUser().getFullName(),
                    category,
                    b.getWantedCount(),
                    b.getPassesDone(),
                    isCurrent
            ));
        }
        return result;
    }

    // 3. Нажать "Сдал"
    @PostMapping("/pass")
    @Transactional
    public String passLab(@PathVariable Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByStudentId(studentId).orElseThrow();

        Booking booking = session.getCurrentBooking();
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Сейчас не ваш ход!");
        }

        // Создаем запись о сдаче
        int p = labPassRepository.countByUserIdAndSubjectId(user.getId(), session.getSubject().getId());
        LabPass labPass = new LabPass();
        labPass.setUser(user);
        labPass.setSubject(session.getSubject());
        labPass.setLabNumber(p + 1);
        labPass.setSession(session);
        labPassRepository.save(labPass);

        // Увеличиваем счетчик
        booking.setPassesDone(booking.getPassesDone() + 1);
        bookingRepository.save(booking);

        // Пересборка очереди
        queueService.advanceTurn(sessionId);
        return "Лаба сдана! Очередь пересобрана.";
    }

    // 4. Пропустить (опоздал)
    @PostMapping("/skip")
    @Transactional
    public String skip(@PathVariable Long sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByStudentId(studentId).orElseThrow();

        Booking booking = session.getCurrentBooking();
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Сейчас не ваш ход!");
        }

        booking.setSkippedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        queueService.advanceTurn(sessionId);
        return "Вы пропущены.";
    }
}