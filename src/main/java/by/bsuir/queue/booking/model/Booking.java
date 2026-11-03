package by.bsuir.queue.booking.model;

import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Время записи задаётся приложением (а не БД), чтобы все времена были по одним часам. */
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "wanted_count", nullable = false)
    private Integer wantedCount;

    @Column(name = "passes_done", nullable = false)
    private Integer passesDone = 0;

    /** Когда студента последний раз пропустили (для аудита). */
    @Column(name = "skipped_at")
    private LocalDateTime skippedAt;

    /** Когда бронь ушла в конец очереди (пропуск или сдача одной из нескольких лаб). */
    @Column(name = "requeued_at")
    private LocalDateTime requeuedAt;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @PrePersist
    void onCreate() {
        if (bookedAt == null) bookedAt = LocalDateTime.now();
    }
}
