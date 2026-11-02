package by.bsuir.queue.booking.model;

import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
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

    @Column(name = "booked_at", insertable = false, updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "wanted_count", nullable = false)
    private Integer wantedCount;

    @Column(name = "passes_done", nullable = false)
    private Integer passesDone = 0;

    @Column(name = "skipped_at")
    private LocalDateTime skippedAt;

    @Version
    @Column(nullable = false)
    private Integer version = 0;
}