package com.daniel.punchclock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class WorkRecord {
    enum Level {
        PENDING,
        GREEN,
        ORANGE,
        RED
    }

    final LocalDate day;
    LocalDateTime clockIn;
    LocalDateTime clockOut;

    WorkRecord(LocalDate day) {
        this.day = day;
    }

    LocalDateTime effectiveClockIn(WorkSettings settings) {
        if (clockIn == null) {
            return null;
        }
        LocalDateTime earliest = clockIn.toLocalDate().atTime(settings.earliestBillableStartTime());
        return clockIn.isBefore(earliest) ? earliest : clockIn;
    }

    LocalDateTime plannedClockOut(WorkSettings settings) {
        LocalDateTime effectiveStart = effectiveClockIn(settings);
        return effectiveStart == null ? null : effectiveStart.plusMinutes(settings.targetPresenceMinutes());
    }

    LocalDateTime safeClockOut(WorkSettings settings) {
        LocalDateTime planned = plannedClockOut(settings);
        return planned == null ? null : planned.plusMinutes(settings.safetyBufferMinutes());
    }

    long presenceMinutes(WorkSettings settings) {
        if (clockIn == null || clockOut == null) {
            return -1;
        }
        return Math.max(0, Duration.between(effectiveClockIn(settings), clockOut).toMinutes());
    }

    long workedMinutes(WorkSettings settings) {
        long presence = presenceMinutes(settings);
        if (presence < 0) {
            return -1;
        }
        return Math.max(0, presence - (settings.deductLunch() ? settings.lunchMinutes() : 0));
    }

    long overtimeMinutes(WorkSettings settings) {
        long worked = workedMinutes(settings);
        if (worked < 0) {
            return -1;
        }
        return Math.max(0, worked - settings.requiredMinutes());
    }

    Level level(WorkSettings settings) {
        long overtime = overtimeMinutes(settings);
        if (overtime < 0) {
            return Level.PENDING;
        }
        if (overtime <= 10) {
            return Level.GREEN;
        }
        if (overtime <= 30) {
            return Level.ORANGE;
        }
        return Level.RED;
    }
}
