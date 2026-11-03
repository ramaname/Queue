package by.bsuir.queue.queue;

import by.bsuir.queue.booking.model.Booking;
import by.bsuir.queue.booking.repository.BookingRepository;
import by.bsuir.queue.lab.repository.LabPassRepository;
import by.bsuir.queue.queue.service.QueueService;
import by.bsuir.queue.session.model.Session;
import by.bsuir.queue.session.repository.SessionRepository;
import by.bsuir.queue.subject.model.Subject;
import by.bsuir.queue.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Включаем Mockito для имитации БД
class QueueServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private LabPassRepository labPassRepository;

    @InjectMocks
    private QueueService queueService; // Тестируемый сервис

    private Session session;
    private User debtor, deadliner, fast;

    @BeforeEach
    void setUp() {
        // 1. Настраиваем сессию: сдаем 5-ю лабу по предмету ID=1
        Subject subject = new Subject();
        subject.setId(1L);

        session = new Session();
        session.setId(1L);
        session.setSubject(subject);
        session.setLabNumber(5); // Целевая лаба K = 5

        // 2. Создаем трех виртуальных студентов
        debtor = new User(); debtor.setId(10L); debtor.setFullName("Должник");
        deadliner = new User(); deadliner.setId(20L); deadliner.setFullName("Дедлайнщик");
        fast = new User(); fast.setId(30L); fast.setFullName("Быстрый");
    }

    @Test
    void testBuildQueue_ShouldInterleaveAndSortCorrectly() {
        // 3. Создаем виртуальные брони (у всех wantedCount = 1, passesDone = 0)
        Booking bDebtor = createBooking(1L, debtor, LocalDateTime.now().minusMinutes(10)); // Забронировал раньше всех
        Booking bDeadliner = createBooking(2L, deadliner, LocalDateTime.now().minusMinutes(5));
        Booking bFast = createBooking(3L, fast, LocalDateTime.now().minusMinutes(1)); // Забронировал позже всех

        // 4. Учим Mockito, что должен ответить репозиторий броней
        when(bookingRepository.findAll()).thenReturn(Arrays.asList(bDebtor, bDeadliner, bFast));

        // 5. Учим Mockito, сколько лаб сдал каждый (имитируем запрос к LabPassRepository)
        // Должник сдал 1 лабу (1 < 4) -> DEBTOR
        when(labPassRepository.countByUserIdAndSubjectId(10L, 1L)).thenReturn(1);
        // Дедлайнщик сдал 4 лабы (4 == 5-1) -> DEADLINER
        when(labPassRepository.countByUserIdAndSubjectId(20L, 1L)).thenReturn(4);
        // Быстрый сдал 6 лаб (6 > 5) -> FAST
        when(labPassRepository.countByUserIdAndSubjectId(30L, 1L)).thenReturn(6);

        // 6. ВЫЗЫВАЕМ ТОТ САМЫЙ АЛГОРИТМ
        List<Booking> queue = queueService.buildQueue(session);

        // 7. ПРОВЕРЯЕМ РЕЗУЛЬТАТ
        assertEquals(3, queue.size(), "В очереди должно быть 3 человека");

        // По правилам: Дедлайнщик первый, затем Должник, затем Быстрый
        assertEquals("Дедлайнщик", queue.get(0).getUser().getFullName(), "Первым должен идти дедлайнщик");
        assertEquals("Должник", queue.get(1).getUser().getFullName(), "Вторым должен идти должник");
        assertEquals("Быстрый", queue.get(2).getUser().getFullName(), "Третьим (в конце) должен идти быстрый");
    }

    // Вспомогательный метод для создания брони
    private Booking createBooking(Long id, User user, LocalDateTime bookedAt) {
        Booking b = new Booking();
        b.setId(id);
        b.setSession(session);
        b.setUser(user);
        b.setBookedAt(bookedAt);
        b.setWantedCount(1);
        b.setPassesDone(0);
        return b;
    }
}