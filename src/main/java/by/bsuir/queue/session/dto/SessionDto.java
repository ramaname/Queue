package by.bsuir.queue.session.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SessionDto {
    private Long id;
    private String subjectName;
    private Integer labNumber;
    private LocalDate deadlineDate;
    private LocalDateTime unlockAt;
    private String status;
}