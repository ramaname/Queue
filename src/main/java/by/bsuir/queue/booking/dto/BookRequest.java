package by.bsuir.queue.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequest {

    @NotNull(message = "Укажите, сколько лаб хотите сдать")
    @Min(value = 1, message = "Нужно сдавать хотя бы одну лабу")
    @Max(value = 50, message = "Слишком большое количество лаб")
    private Integer wantedCount;
}
