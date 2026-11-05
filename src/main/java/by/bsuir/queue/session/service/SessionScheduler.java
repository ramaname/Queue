package by.bsuir.queue.session.service;

import by.bsuir.queue.queue.service.QueueService;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "queue.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionScheduler.class);

    private final SessionRepository sessionRepository;
    private final QueueService queueService;

    public SessionScheduler(SessionRepository sessionRepository, QueueService queueService) {
        this.sessionRepository = sessionRepository;
        this.queueService = queueService;
    }

    @Scheduled(fixedRate = 10000)
    public void tick() {
        for (Session s : sessionRepository.findByStatus(QueueService.SCHEDULED)) {
            safely(() -> queueService.openIfDue(s.getId()), s.getId());
        }
        for (Session s : sessionRepository.findByStatus(QueueService.OPEN)) {
            safely(() -> queueService.closeWindowIfDue(s.getId()), s.getId());
        }
        for (Session s : sessionRepository.findByStatus(QueueService.CLOSED)) {
            safely(() -> queueService.autoSkipIfExpired(s.getId()), s.getId());
        }
    }

    private void safely(Runnable action, Long sessionId) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Ошибка планировщика для сессии {}", sessionId, e);
        }
    }
}
