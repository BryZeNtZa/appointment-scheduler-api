package com.afb.scheduler.appointment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotTest {

    @Test
    void listsEightHourlySlotsFrom8To15() {
        assertThat(TimeSlot.all()).hasSize(8);
        assertThat(TimeSlot.all().getFirst().getHour()).isEqualTo(8);
        assertThat(TimeSlot.all().getLast().getHour()).isEqualTo(15);
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 9, 12, 15})
    void acceptsOnTheHourStartsWithinRange(int hour) {
        assertThat(TimeSlot.isValidStart(LocalDateTime.of(2026, 6, 1, hour, 0))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 16, 17})
    void rejectsHoursOutsideRange(int hour) {
        assertThat(TimeSlot.isValidStart(LocalDateTime.of(2026, 6, 1, hour, 0))).isFalse();
    }

    @Test
    void rejectsStartsThatAreNotOnTheHour() {
        assertThat(TimeSlot.isValidStart(LocalDateTime.of(2026, 6, 1, 9, 30))).isFalse();
    }

    @Test
    void truncatesToSlotStart() {
        assertThat(TimeSlot.slotStartOf(LocalDateTime.of(2026, 6, 1, 9, 45, 12)))
                .isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
    }
}
