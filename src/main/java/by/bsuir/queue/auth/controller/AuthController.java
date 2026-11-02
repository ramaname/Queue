package by.bsuir.queue.auth.controller;

import by.bsuir.queue.auth.dto.AuthResponse;
import by.bsuir.queue.auth.dto.LoginRequest;
import by.bsuir.queue.auth.dto.RegisterRequest;
import by.bsuir.queue.auth.service.JwtService;
import by.bsuir.queue.user.model.User;
import by.bsuir.queue.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (userRepository.findByStudentId(request.getStudentId()).isPresent()) {
            throw new RuntimeException("Студент с таким билетом уже существует");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getStudentId())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();

        String jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getStudentId(), request.getPassword())
        );

        User user = userRepository.findByStudentId(request.getStudentId()).orElseThrow();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getStudentId())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();

        String jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken);
    }
}