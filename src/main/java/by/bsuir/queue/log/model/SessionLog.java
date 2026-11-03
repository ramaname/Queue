package by.bsuir.queue.log.model;

import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session_log")
public class SessionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}