package by.bsuir.queue.session.model;

import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "lab_number", nullable = false)
    private Integer labNumber;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "unlock_at", nullable = false)
    private LocalDateTime unlockAt;

    @Column(name = "window_minutes", nullable = false)
    private Integer windowMinutes = 10;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_booking_id")
    private Booking currentBooking;

    @Column(name = "current_turn_started_at")
    private LocalDateTime currentTurnStartedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
