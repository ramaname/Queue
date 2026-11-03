package by.bsuir.queue.auth.controller;

import by.bsuir.queue.auth.dto.AuthResponse;
import by.bsuir.queue.auth.dto.LoginRequest;
import by.bsuir.queue.auth.dto.MeResponse;
import by.bsuir.queue.auth.dto.RegisterRequest;
import by.bsuir.queue.auth.service.JwtService;
import by.bsuir.queue.common.ApiException;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String studentId = request.getStudentId().trim();
        // коллация utf8mb4_unicode_ci регистронезависима, поэтому «21b0452» и «21B0452» — один студент
        if (userRepository.findByStudentId(studentId).isPresent()) {
            throw ApiException.conflict("Студент с таким номером уже зарегистрирован");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim().replaceAll("\\s+", " "));
        user.setStudentId(studentId);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);   // гонку двух одновременных регистраций ловит UNIQUE → 409 в обработчике
        return new AuthResponse(jwtService.generateToken(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String studentId = request.getStudentId().trim();
        // неверные данные → AuthenticationException → 401 в GlobalExceptionHandler
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(studentId, request.getPassword()));
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> ApiException.unauthorized("Неверный номер студента или пароль"));
        return new AuthResponse(jwtService.generateToken(user));
    }

    /** Профиль по токену — на случай старых токенов без ФИО. */
    @GetMapping("/me")
    public MeResponse me(Principal principal) {
        User user = userRepository.findByStudentId(principal.getName())
                .orElseThrow(() -> ApiException.unauthorized("Пользователь не найден. Войдите заново."));
        return new MeResponse(user.getFullName(), user.getStudentId(), user.getRole());
    }
}
