package by.bsuir.queue.lab.model;

import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lab_passes")
public class LabPass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "lab_number", nullable = false)
    private Integer labNumber;

    @Column(name = "passed_at", insertable = false, updatable = false)
    private LocalDateTime passedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;
}
