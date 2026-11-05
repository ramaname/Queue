package by.bsuir.queue.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateSessionRequest {

    @NotNull(message = "Выберите предмет")
    private Long subjectId;

    @NotNull(message = "Укажите номер лабораторной")
    @Min(value = 1, message = "Номер лабораторной должен быть положительным")
    private Integer labNumber;

    @NotNull(message = "Укажите дедлайн")
    private LocalDate deadlineDate;

    @NotNull(message = "Укажите время открытия записи")
    private LocalDateTime unlockAt;

    @Min(value = 1, message = "Окно записи — минимум 1 минута")
    @Max(value = 1440, message = "Окно записи — максимум 1440 минут")
    private Integer windowMinutes;
}
