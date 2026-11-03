package by.bsuir.queue.queue.service;

import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.common.ApiException;
import by.bsuir.queue.lab.model.LabPass;
import by.bsuir.queue.lab.repository.LabPassRepository;
import by.bsuir.queue.queue.dto.QueueItemDto;
import by.bsuir.queue.queue.service.QueueOrdering.Category;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Вся логика очереди. Любое изменение сессии идёт под блокировкой её строки в БД
 * (SELECT … FOR UPDATE), поэтому два одновременных «сдал», «сдал» и «пропустить»
 * или планировщик с пользователем не могут испортить очередь.
 *
 * Жизненный цикл: SCHEDULED → OPEN (идёт запись, очерёдности ещё нет) →
 * CLOSED (окно закрыто, очередь строится, студенты сдают по одному) → FINISHED.
 */
@Service
public class QueueService {

    public static final String SCHEDULED = "SCHEDULED";
    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";
    public static final String FINISHED = "FINISHED";

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    private final BookingRepository bookingRepository;
    private final SessionRepository sessionRepository;
    private final LabPassRepository labPassRepository;
    private final UserRepository userRepository;
    private final long turnTimeoutMinutes;

    public QueueService(BookingRepository bookingRepository,
                        SessionRepository sessionRepository,
                        LabPassRepository labPassRepository,
                        UserRepository userRepository,
                        @Value("${queue.turn-timeout-minutes:3}") long turnTimeoutMinutes) {
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.labPassRepository = labPassRepository;
        this.userRepository = userRepository;
        this.turnTimeoutMinutes = turnTimeoutMinutes;
    }

    /** Строка очереди: бронь + вычисленная категория. */
    public record QueueRow(Booking booking, Category category) { }

    /* ====================================================================
       Построение очереди
       ==================================================================== */

    /** Очередь сессии в порядке сдачи (только те, кто ещё не сдал всё, что хотел). */
    @Transactional(readOnly = true)
    public List<QueueRow> buildRows(Session session) {
        List<Booking> active = bookingRepository.findBySessionIdWithUser(session.getId()).stream()
                .filter(b -> b.getPassesDone() < b.getWantedCount())
                .collect(Collectors.toList());
        if (active.isEmpty()) return List.of();

        // одним запросом узнаём, сколько лаб сдал каждый (раньше — отдельный запрос на каждого)
        List<Long> userIds = active.stream().map(b -> b.getUser().getId()).collect(Collectors.toList());
        Map<Long, Integer> passed = new HashMap<>();
        for (Object[] row : labPassRepository.countByUsers(session.getSubject().getId(), userIds)) {
            passed.put((Long) row[0], ((Number) row[1]).intValue());
        }

        Map<Long, Booking> byId = new HashMap<>();
        Map<Long, Category> categories = new HashMap<>();
        List<QueueOrdering.Entry> entries = new ArrayList<>();
        for (Booking b : active) {
            Category c = QueueOrdering.categorize(passed.getOrDefault(b.getUser().getId(), 0), session.getLabNumber());
            byId.put(b.getId(), b);
            categories.put(b.getId(), c);
            entries.add(new QueueOrdering.Entry(b.getId(), b.getBookedAt(), b.getRequeuedAt(), c));
        }
        List<QueueRow> rows = new ArrayList<>();
        for (Long id : QueueOrdering.order(entries)) rows.add(new QueueRow(byId.get(id), categories.get(id)));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Booking> buildQueue(Session session) {
        return buildRows(session).stream().map(QueueRow::booking).collect(Collectors.toList());
    }

    /**
     * Назначает текущего сдающего. Очередь «оживает» только после закрытия окна записи (CLOSED).
     * Пока идёт запись — никто не ходит, иначе первый записавшийся обходил бы приоритеты.
     * Когда в закрытой сессии никого не осталось — она завершается.
     */
    @Transactional
    public void advanceTurn(Long sessionId) {
        Session session = lock(sessionId);
        if (FINISHED.equals(session.getStatus())) return;

        if (!CLOSED.equals(session.getStatus())) {
            session.setCurrentBooking(null);
            session.setCurrentTurnStartedAt(null);
            sessionRepository.save(session);
            return;
        }

        List<QueueRow> rows = buildRows(session);
        if (rows.isEmpty()) {
            session.setStatus(FINISHED);
            session.setClosedAt(LocalDateTime.now());
            session.setCurrentBooking(null);
            session.setCurrentTurnStartedAt(null);
            log.info("Сессия {} завершена: очередь пуста", sessionId);
        } else {
            session.setCurrentBooking(rows.get(0).booking());
            session.setCurrentTurnStartedAt(LocalDateTime.now());
        }
        sessionRepository.save(session);
    }

    /* ====================================================================
       Действия студента
       ==================================================================== */

    @Transactional
    public void book(Long sessionId, String studentId, Integer wantedCount) {
        Session session = lock(sessionId);
        User user = currentUser(studentId);

        if (!OPEN.equals(session.getStatus())) {
            throw ApiException.conflict("Запись в эту сессию сейчас не открыта");
        }
        if (bookingRepository.existsBySessionIdAndUserId(sessionId, user.getId())) {
            throw ApiException.conflict("Вы уже записаны в эту очередь");
        }
        if (wantedCount == null || wantedCount < 1) {
            throw ApiException.badRequest("Нужно сдавать хотя бы одну лабу");
        }

        int total = session.getSubject().getTotalLabs();
        int passed = labPassRepository.countByUserIdAndSubjectId(user.getId(), session.getSubject().getId());
        if (passed >= total) {
            throw ApiException.conflict("Вы уже сдали все лабораторные по этому предмету");
        }
        if (wantedCount > total - passed) {
            throw ApiException.badRequest("Можно записаться максимум на " + (total - passed) + " лаб (осталось несданных)");
        }

        Booking booking = new Booking();
        booking.setSession(session);
        booking.setUser(user);
        booking.setWantedCount(wantedCount);
        booking.setPassesDone(0);
        booking.setBookedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Transactional
    public String pass(Long sessionId, String studentId) {
        Session session = lock(sessionId);
        User user = currentUser(studentId);
        Booking current = requireMyTurn(session, user);

        int total = session.getSubject().getTotalLabs();
        int passed = labPassRepository.countByUserIdAndSubjectId(user.getId(), session.getSubject().getId());
        if (passed >= total) {
            throw ApiException.conflict("Все лабораторные по этому предмету уже сданы");
        }

        LabPass labPass = new LabPass();
        labPass.setUser(user);
        labPass.setSubject(session.getSubject());
        labPass.setLabNumber(passed + 1);
        labPass.setSession(session);
        labPassRepository.save(labPass);

        current.setPassesDone(current.getPassesDone() + 1);
        // сдал не всё, что хотел — едет в конец очереди, чтобы не занимать место подряд
        if (current.getPassesDone() < current.getWantedCount()) current.setRequeuedAt(LocalDateTime.now());
        bookingRepository.save(current);

        advanceTurn(sessionId);
        return "Лаба сдана!";
    }

    @Transactional
    public String skip(Long sessionId, String studentId) {
        Session session = lock(sessionId);
        User user = currentUser(studentId);
        Booking current = requireMyTurn(session, user);
        sendToBack(current);
        advanceTurn(sessionId);
        return "Вы пропущены.";
    }

    /* ====================================================================
       Действия администратора и планировщика
       ==================================================================== */

    @Transactional
    public String adminSkip(Long sessionId) {
        Session session = lock(sessionId);
        Booking current = session.getCurrentBooking();
        if (!CLOSED.equals(session.getStatus()) || current == null) {
            throw ApiException.conflict("Сейчас никто не сдаёт");
        }
        String name = current.getUser().getFullName();
        sendToBack(current);
        advanceTurn(sessionId);
        return "Студент " + name + " пропущен администратором.";
    }

    @Transactional
    public void finishSession(Long sessionId) {
        Session session = lock(sessionId);
        session.setStatus(FINISHED);
        session.setClosedAt(LocalDateTime.now());
        session.setCurrentBooking(null);
        session.setCurrentTurnStartedAt(null);
        sessionRepository.save(session);
    }

    /** SCHEDULED → OPEN, когда пришло время открытия. */
    @Transactional
    public void openIfDue(Long sessionId) {
        Session s = lock(sessionId);
        if (SCHEDULED.equals(s.getStatus()) && !s.getUnlockAt().isAfter(LocalDateTime.now())) {
            s.setStatus(OPEN);
            sessionRepository.save(s);
            log.info("Сессия {} открыта для записи", sessionId);
        }
    }

    /** OPEN → CLOSED по истечении окна записи; сразу назначаем первого в очереди. */
    @Transactional
    public void closeWindowIfDue(Long sessionId) {
        Session s = lock(sessionId);
        if (OPEN.equals(s.getStatus()) && LocalDateTime.now().isAfter(s.getUnlockAt().plusMinutes(s.getWindowMinutes()))) {
            s.setStatus(CLOSED);
            sessionRepository.save(s);
            log.info("Окно записи сессии {} закрыто", sessionId);
            advanceTurn(sessionId);
        }
    }

    /** Автопропуск, если текущий не уложился в отведённое время. */
    @Transactional
    public void autoSkipIfExpired(Long sessionId) {
        Session s = lock(sessionId);
        if (!CLOSED.equals(s.getStatus())) return;
        Booking current = s.getCurrentBooking();
        if (current == null) {            // страховка: идёт сдача, а текущего нет (например, после сбоя)
            advanceTurn(sessionId);
            return;
        }
        if (s.getCurrentTurnStartedAt() == null) return;
        if (LocalDateTime.now().isAfter(s.getCurrentTurnStartedAt().plusMinutes(turnTimeoutMinutes))) {
            log.info("Таймаут хода у {} (сессия {}): автопропуск", current.getUser().getStudentId(), sessionId);
            sendToBack(current);
            advanceTurn(sessionId);
        }
    }

    /* ====================================================================
       Чтение для фронтенда
       ==================================================================== */

    @Transactional(readOnly = true)
    public List<QueueItemDto> getQueue(Long sessionId, String studentId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Сессия не найдена"));
        Long currentId = session.getCurrentBooking() == null ? null : session.getCurrentBooking().getId();

        List<QueueRow> rows = buildRows(session);
        List<QueueItemDto> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Booking b = rows.get(i).booking();
            result.add(new QueueItemDto(
                    i + 1,
                    b.getUser().getFullName(),
                    rows.get(i).category().name(),
                    b.getWantedCount(),
                    b.getPassesDone(),
                    b.getId().equals(currentId),
                    b.getUser().getStudentId().equalsIgnoreCase(studentId)));
        }
        return result;
    }

    /* ====================================================================
       Вспомогательное
       ==================================================================== */

    private Session lock(Long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("Сессия не найдена"));
    }

    private User currentUser(String studentId) {
        return userRepository.findByStudentId(studentId)
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден. Войдите заново."));
    }

    private Booking requireMyTurn(Session session, User user) {
        Booking current = session.getCurrentBooking();
        if (!CLOSED.equals(session.getStatus()) || current == null || !current.getUser().getId().equals(user.getId())) {
            throw ApiException.conflict("Сейчас не ваш ход");
        }
        return current;
    }

    private void sendToBack(Booking booking) {
        LocalDateTime now = LocalDateTime.now();
        booking.setSkippedAt(now);
        booking.setRequeuedAt(now);
        bookingRepository.save(booking);
    }
}
