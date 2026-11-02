package by.bsuir.queue.auth.dto;
import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String studentId;
    private String password;
}