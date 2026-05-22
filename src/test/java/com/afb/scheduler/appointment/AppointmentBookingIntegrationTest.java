package com.afb.scheduler.appointment;

import com.afb.scheduler.TestcontainersConfiguration;
import com.afb.scheduler.appointment.dto.BookAppointmentRequest;
import com.afb.scheduler.common.error.ConflictException;
import com.afb.scheduler.user.Role;
import com.afb.scheduler.user.User;
import com.afb.scheduler.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AppointmentBookingIntegrationTest {

    private static final LocalDateTime FUTURE_SLOT = LocalDate.now().plusDays(7).atTime(9, 0);

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void seedUsers() {
        appointmentRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User("MGR-1", "mgr@afb.test", "0600000001", "Doe", "John", Role.MANAGER));
        userRepository.save(new User("CLI-1", "cli1@afb.test", "0600000002", "Roe", "Jane", Role.CLIENT));
        userRepository.save(new User("CLI-2", "cli2@afb.test", "0600000003", "Smith", "Sam", Role.CLIENT));
    }

    private BookAppointmentRequest request(String clientRef) {
        return new BookAppointmentRequest(null, clientRef, null, "HR", "MGR-1", FUTURE_SLOT, "Annual review");
    }

    @Test
    void persistsAppointmentThroughTheFullTransactionFlow() {
        Appointment booked = appointmentService.book(request("CLI-1"));

        Appointment found = appointmentService.getByRef(booked.getRefRdv());
        assertThat(found.getManager().getRef()).isEqualTo("MGR-1");
        assertThat(found.getDepartment().getCode()).isEqualTo("HR");
        assertThat(found.getParticipants()).hasSize(1);
        assertThat(found.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(found.getSlotStart()).isEqualTo(FUTURE_SLOT);
    }

    @Test
    void allowsOnlyOneBookingForTheSameManagerAndSlotUnderConcurrency() throws Exception {
        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        String[] clientRefs = {"CLI-1", "CLI-2"};

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String clientRef = clientRefs[i];
            futures.add(pool.submit(() -> {
                startGate.await();
                try {
                    appointmentService.book(request(clientRef));
                    successes.incrementAndGet();
                } catch (ConflictException expected) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown();
        for (Future<?> future : futures) {
            future.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(appointmentRepository.findByStatusOrderByDateRdvAsc(AppointmentStatus.BOOKED)).hasSize(1);
    }
}
