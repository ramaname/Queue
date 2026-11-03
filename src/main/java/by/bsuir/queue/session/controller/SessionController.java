package by.bsuir.queue.session.controller;

import by.bsuir.queue.session.dto.CreateSessionRequest;
import by.bsuir.queue.session.dto.SessionDto;
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<SessionDto> getAllSessions() {
        return sessionRepository.findAll().stream().map(s -> {
            SessionDto dto = new SessionDto();
            dto.setId(s.getId());
            dto.setSubjectName(s.getSubject().getName());
            dto.setLabNumber(s.getLabNumber());
            dto.setDeadlineDate(s.getDeadlineDate());
            dto.setUnlockAt(s.getUnlockAt());
            dto.setStatus(s.getStatus());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }
}