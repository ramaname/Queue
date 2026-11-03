package by.bsuir.queue.session.controller;

import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.common.ApiException;
import by.bsuir.queue.queue.service.QueueService;
import by.bsuir.queue.session.dto.CreateSessionRequest;
import by.bsuir.queue.session.dto.SessionDto;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.subject.repository.SubjectRepository;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final QueueService queueService;

    public SessionController(SessionRepository sessionRepository, SubjectRepository subjectRepository,
                             UserRepository userRepository, BookingRepository bookingRepository,
                             QueueService queueService) {
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.queueService = queueService;
    }

    /** Только ADMIN (см. SecurityConfig). */
    @PostMapping
    @Transactional
    public SessionDto createSession(@Valid @RequestBody CreateSessionRequest request, Principal principal) {
        User admin = userRepository.findByStudentId(principal.getName())
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден. Войдите заново."));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> ApiException.notFound("Предмет не найден"));
        if (request.getLabNumber() > subject.getTotalLabs()) {
            throw ApiException.badRequest("У предмета «" + subject.getName() + "» всего " + subject.getTotalLabs() + " лаб");
        }

        Session session = new Session();
        session.setSubject(subject);
        session.setLabNumber(request.getLabNumber());
        session.setDeadlineDate(request.getDeadlineDate());
        session.setUnlockAt(request.getUnlockAt());
        session.setWindowMinutes(request.getWindowMinutes() != null ? request.getWindowMinutes() : 10);
        session.setStatus(QueueService.SCHEDULED);
        session.setCreatedBy(admin);
        return SessionDto.of(sessionRepository.save(session), false);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SessionDto> getAllSessions(Principal principal) {
        Set<Long> mine = new HashSet<>(bookingRepository.findSessionIdsByStudentId(principal.getName()));
        return sessionRepository.findAll().stream()
                .map(s -> SessionDto.of(s, mine.contains(s.getId())))
                .collect(Collectors.toList());
    }

    /** Только ADMIN. */
    @DeleteMapping("/{id}")
    @Transactional
    public void deleteSession(@PathVariable Long id) {
        if (!sessionRepository.existsById(id)) throw ApiException.notFound("Сессия не найдена");
        sessionRepository.deleteById(id);
    }

    /** Только ADMIN: досрочное завершение. */
    @PostMapping("/{id}/finish")
    public void finishSession(@PathVariable Long id) {
        queueService.finishSession(id);
    }
}
