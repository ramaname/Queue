package by.bsuir.queue.queue.controller;

import by.bsuir.queue.booking.dto.BookRequest;
import by.bsuir.queue.queue.dto.QueueItemDto;
import by.bsuir.queue.queue.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Тонкий контроллер: вся логика и транзакции — в QueueService. */
@RestController
@RequestMapping("/api/sessions/{sessionId}")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/book")
    public String book(@PathVariable Long sessionId, @Valid @RequestBody BookRequest request, Principal principal) {
        queueService.book(sessionId, principal.getName(), request.getWantedCount());
        return "Вы успешно заняли очередь!";
    }

    @GetMapping("/queue")
    public List<QueueItemDto> getQueue(@PathVariable Long sessionId, Principal principal) {
        return queueService.getQueue(sessionId, principal.getName());
    }

    @PostMapping("/pass")
    public String pass(@PathVariable Long sessionId, Principal principal) {
        return queueService.pass(sessionId, principal.getName());
    }

    @PostMapping("/skip")
    public String skip(@PathVariable Long sessionId, Principal principal) {
        return queueService.skip(sessionId, principal.getName());
    }

    /** Только ADMIN (см. SecurityConfig). */
    @PostMapping("/admin-skip")
    public String adminSkip(@PathVariable Long sessionId) {
        return queueService.adminSkip(sessionId);
    }
}
