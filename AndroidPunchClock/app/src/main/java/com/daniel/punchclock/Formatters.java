package com.daniel.punchclock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Formatters {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.TRADITIONAL_CHINESE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M 月 d 日 EEEE", Locale.TRADITIONAL_CHINESE);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 EEEE", Locale.TRADITIONAL_CHINESE);

    private Formatters() {
    }

    static String time(LocalDateTime dateTime) {
        return dateTime == null ? "--:--" : TIME.format(dateTime);
    }

    static String time(LocalTime time) {
        return time == null ? "--:--" : TIME.format(time);
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }

    static String fullDate(LocalDate date) {
        return FULL_DATE.format(date);
    }

    static String workedTime(long minutes) {
        if (minutes < 0) {
            return "--";
        }
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
    }

    static String remainingMinutes(long minutes) {
        if (minutes < 60) {
            return minutes + " 分鐘";
        }
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
    }
}
