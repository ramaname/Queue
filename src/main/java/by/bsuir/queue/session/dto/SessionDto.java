package by.bsuir.queue.session.dto;

import by.bsuir.queue.session.model.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SessionDto(
        Long id,
        String subjectName,
        Integer labNumber,
        LocalDate deadlineDate,
        LocalDateTime unlockAt,
        Integer windowMinutes,
        String status,
        boolean booked) {

    public static SessionDto of(Session s, boolean booked) {
        return new SessionDto(s.getId(), s.getSubject().getName(), s.getLabNumber(), s.getDeadlineDate(),
                s.getUnlockAt(), s.getWindowMinutes(), s.getStatus(), booked);
    }
}
