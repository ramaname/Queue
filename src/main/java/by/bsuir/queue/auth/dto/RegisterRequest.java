package by.bsuir.queue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Введите ФИО")
    @Size(min = 2, max = 200, message = "ФИО: от 2 до 200 символов")
    private String fullName;

    @NotBlank(message = "Введите номер студента")
    @Size(max = 20, message = "Номер студента: не более 20 символов")
    private String studentId;

    @NotBlank(message = "Введите пароль")
    @Size(min = 6, max = 72, message = "Пароль: от 6 до 72 символов")
    private String password;
}
