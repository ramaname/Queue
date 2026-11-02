package by.bsuir.queue.session.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CreateSessionRequest {
    private Long subjectId;
    private Integer labNumber;
    private LocalDate deadlineDate;
    private LocalDateTime unlockAt;
    private Integer windowMinutes; // если null, ставим 10
}