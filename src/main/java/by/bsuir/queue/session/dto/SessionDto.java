package by.bsuir.queue.session.dto;

import by.bsuir.queue.session.model.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сессия для фронтенда. booked — текущий пользователь уже записан в эту очередь.
 * Entity наружу не отдаём: Jackson тащил lazy-связи и хэш пароля создателя.
 */
public record SessionDto(
        Long id,
        String subjectName,
        Integer labNumber,
        LocalDate deadlineDate,
        LocalDateTime unlockAt,
        Integer windowMinutes,
        String status,
        boolean booked) {

    /** Вызывать внутри транзакции (читает lazy-предмет). */
    public static SessionDto of(Session s, boolean booked) {
        return new SessionDto(s.getId(), s.getSubject().getName(), s.getLabNumber(), s.getDeadlineDate(),
                s.getUnlockAt(), s.getWindowMinutes(), s.getStatus(), booked);
    }
}
