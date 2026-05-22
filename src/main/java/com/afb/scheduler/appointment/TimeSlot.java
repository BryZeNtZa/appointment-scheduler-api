package com.afb.scheduler.appointment;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Bookable slots are one-hour blocks from 08:00 to 16:00, so valid start times are
 * 08:00, 09:00, ... 15:00 (the last block ends at 16:00).
 */
public final class TimeSlot {

    public static final int FIRST_START_HOUR = 8;
    public static final int LAST_START_HOUR = 15;

    private TimeSlot() {
    }

    public static List<LocalTime> all() {
        return IntStream.rangeClosed(FIRST_START_HOUR, LAST_START_HOUR)
                .mapToObj(hour -> LocalTime.of(hour, 0))
                .toList();
    }

    public static boolean isValidStart(LocalDateTime dateTime) {
        return dateTime.getMinute() == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && dateTime.getHour() >= FIRST_START_HOUR
                && dateTime.getHour() <= LAST_START_HOUR;
    }

    public static LocalDateTime slotStartOf(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.HOURS);
    }
}
