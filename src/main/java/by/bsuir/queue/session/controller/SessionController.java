package by.bsuir.queue.session.controller;

import by.bsuir.queue.session.dto.CreateSessionRequest;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.subject.repository.SubjectRepository;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public SessionController(SessionRepository sessionRepository, SubjectRepository subjectRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Session createSession(@RequestBody CreateSessionRequest request) {
        // Получаем текущего юзера (админа)
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByStudentId(studentId).orElseThrow();

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Предмет не найден"));

        Session session = new Session();
        session.setSubject(subject);
        session.setLabNumber(request.getLabNumber());
        session.setDeadlineDate(request.getDeadlineDate());
        session.setUnlockAt(request.getUnlockAt());
        session.setWindowMinutes(request.getWindowMinutes() != null ? request.getWindowMinutes() : 10);
        session.setStatus("SCHEDULED"); // Изначально запланирована
        session.setCreatedBy(admin);

        return sessionRepository.save(session);
    }

    @GetMapping
    public java.util.List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }
}