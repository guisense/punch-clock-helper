package com.daniel.punchclock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    int leaveMinutes;
    Integer leaveStartMinutes;
    Integer leaveEndMinutes;
    boolean leaveIncludesLunch = true;
    Integer billableStartOverrideMinutes;
    Boolean deductLunchOverride;

    WorkRecord(LocalDate day) {
        this.day = day;
    }

    LocalDateTime effectiveClockIn(WorkSettings settings) {
        if (clockIn == null) {
            return null;
        }
        int startMinutes = billableStartOverrideMinutes == null
                ? settings.earliestBillableStartMinutes()
                : billableStartOverrideMinutes;
        LocalDateTime earliest = clockIn.toLocalDate().atTime(startMinutes / 60, startMinutes % 60);
        LocalDateTime effective = clockIn.isBefore(earliest) ? earliest : clockIn;
        int effectiveMinute = minuteOfDay(effective);
        if (leaveEndMinutes != null
                && leaveStartMinutes != null
                && effectiveMinute >= leaveStartMinutes
                && effectiveMinute < leaveEndMinutes) {
            effective = day.atTime(leaveEndMinutes / 60, leaveEndMinutes % 60);
        }
        return nextWorkingTime(effective, settings);
    }

    LocalDateTime plannedClockOut(WorkSettings settings) {
        LocalDateTime effectiveStart = effectiveClockIn(settings);
        if (effectiveStart == null) {
            return null;
        }
        int target = requiredMinutes(settings);
        LocalDateTime cursor = effectiveStart;
        int worked = 0;
        int guard = 0;
        while (worked < target && guard < 36 * 60) {
            if (countsAsWorkMinute(cursor, settings)) {
                worked++;
            }
            cursor = cursor.plusMinutes(1);
            guard++;
        }
        return cursor;
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
        LocalDateTime cursor = effectiveClockIn(settings);
        long worked = 0;
        int guard = 0;
        while (cursor.isBefore(clockOut) && guard < 36 * 60) {
            if (countsAsWorkMinute(cursor, settings)) {
                worked++;
            }
            cursor = cursor.plusMinutes(1);
            guard++;
        }
        return Math.max(0, worked);
    }

    long overtimeMinutes(WorkSettings settings) {
        long worked = workedMinutes(settings);
        if (worked < 0) {
            return -1;
        }
        return Math.max(0, worked - requiredMinutes(settings));
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

    int requiredMinutes(WorkSettings settings) {
        return Math.max(0, settings.requiredMinutes() - leaveCreditMinutes(settings));
    }

    int targetPresenceMinutes(WorkSettings settings) {
        return requiredMinutes(settings);
    }

    boolean deductLunch(WorkSettings settings) {
        return deductLunchOverride == null ? settings.deductLunch() : deductLunchOverride;
    }

    String leaveText() {
        return leaveCreditMinutesText(null);
    }

    String billableStartText(WorkSettings settings) {
        int minutes = billableStartOverrideMinutes == null
                ? settings.earliestBillableStartMinutes()
                : billableStartOverrideMinutes;
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    int leaveCreditMinutes(WorkSettings settings) {
        if (leaveStartMinutes == null || leaveEndMinutes == null || leaveEndMinutes <= leaveStartMinutes) {
            return Math.max(0, leaveMinutes);
        }
        int raw = leaveEndMinutes - leaveStartMinutes;
        if (!leaveIncludesLunch) {
            return raw;
        }
        return Math.max(0, raw - overlapMinutes(leaveStartMinutes, leaveEndMinutes, settings.lunchStartMinutes(), settings.lunchEndMinutes()));
    }

    String leaveText(WorkSettings settings) {
        if (leaveStartMinutes == null || leaveEndMinutes == null || leaveEndMinutes <= leaveStartMinutes) {
            return leaveMinutes <= 0 ? "無" : Formatters.remainingMinutes(leaveMinutes);
        }
        return formatClockTime(leaveStartMinutes) + " - " + formatClockTime(leaveEndMinutes)
                + " · " + Formatters.remainingMinutes(leaveCreditMinutes(settings));
    }

    private String leaveCreditMinutesText(WorkSettings settings) {
        if (settings == null) {
            return leaveMinutes <= 0 && (leaveStartMinutes == null || leaveEndMinutes == null) ? "無" : "已設定";
        }
        return leaveText(settings);
    }

    private boolean countsAsWorkMinute(LocalDateTime time, WorkSettings settings) {
        int minute = minuteOfDay(time);
        boolean excludeLunch = deductLunch(settings) || hasLeavePeriod();
        if (excludeLunch && minute >= settings.lunchStartMinutes() && minute < settings.lunchEndMinutes()) {
            return false;
        }
        return leaveStartMinutes == null || leaveEndMinutes == null || minute < leaveStartMinutes || minute >= leaveEndMinutes;
    }

    private boolean hasLeavePeriod() {
        return leaveStartMinutes != null && leaveEndMinutes != null && leaveEndMinutes > leaveStartMinutes;
    }

    private LocalDateTime nextWorkingTime(LocalDateTime time, WorkSettings settings) {
        LocalDateTime cursor = time;
        int guard = 0;
        while (!countsAsWorkMinute(cursor, settings) && guard < 24 * 60) {
            cursor = cursor.plusMinutes(1);
            guard++;
        }
        return cursor;
    }

    private int minuteOfDay(LocalDateTime time) {
        LocalTime localTime = time.toLocalTime();
        return localTime.getHour() * 60 + localTime.getMinute();
    }

    private int overlapMinutes(int start, int end, int otherStart, int otherEnd) {
        return Math.max(0, Math.min(end, otherEnd) - Math.max(start, otherStart));
    }

    private String formatClockTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
