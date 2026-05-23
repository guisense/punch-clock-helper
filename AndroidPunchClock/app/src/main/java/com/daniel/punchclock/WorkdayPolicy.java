package com.daniel.punchclock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

final class WorkdayPolicy {
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> MAKEUP_WORKDAYS = new HashSet<>();

    static {
        addRange("2026-01-01", "2026-01-03");
        addWorkday("2026-01-04");

        addRange("2026-02-15", "2026-02-23");
        addWorkday("2026-02-14");
        addWorkday("2026-02-28");

        addRange("2026-04-04", "2026-04-06");

        addRange("2026-05-01", "2026-05-05");
        addWorkday("2026-05-09");

        addRange("2026-06-19", "2026-06-21");
        addRange("2026-09-25", "2026-09-27");

        addRange("2026-10-01", "2026-10-07");
        addWorkday("2026-09-20");
        addWorkday("2026-10-10");
    }

    private WorkdayPolicy() {
    }

    static boolean isWorkday(LocalDate date) {
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        if (MAKEUP_WORKDAYS.contains(date)) {
            return true;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private static void addRange(String start, String end) {
        LocalDate current = LocalDate.parse(start);
        LocalDate last = LocalDate.parse(end);
        while (!current.isAfter(last)) {
            HOLIDAYS.add(current);
            current = current.plusDays(1);
        }
    }

    private static void addWorkday(String date) {
        MAKEUP_WORKDAYS.add(LocalDate.parse(date));
    }
}
