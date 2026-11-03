package by.bsuir.queue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Введите номер студента")
    private String studentId;

    @NotBlank(message = "Введите пароль")
    private String password;
}
