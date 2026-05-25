package com.daniel.punchclock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

final class WorkdayPolicy {
    private static final String PREFS = "holiday_rules";
    private static final String RULES_JSON = "rules_json";
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> MAKEUP_WORKDAYS = new HashSet<>();
    private static final Set<LocalDate> EXTERNAL_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> EXTERNAL_MAKEUP_WORKDAYS = new HashSet<>();

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
        if (EXTERNAL_HOLIDAYS.contains(date)) {
            return false;
        }
        if (EXTERNAL_MAKEUP_WORKDAYS.contains(date)) {
            return true;
        }
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        if (MAKEUP_WORKDAYS.contains(date)) {
            return true;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    static void loadExternalRules(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(RULES_JSON, null);
        if (raw == null) {
            return;
        }
        applyExternalRules(raw);
    }

    static void saveExternalRules(Context context, String raw) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(RULES_JSON, raw)
                .apply();
        applyExternalRules(raw);
    }

    private static void applyExternalRules(String raw) {
        try {
            JSONObject root = new JSONObject(raw);
            EXTERNAL_HOLIDAYS.clear();
            EXTERNAL_MAKEUP_WORKDAYS.clear();
            addDates(root.optJSONArray("holidays"), EXTERNAL_HOLIDAYS);
            addDates(root.optJSONArray("makeupWorkdays"), EXTERNAL_MAKEUP_WORKDAYS);
        } catch (Exception ignored) {
        }
    }

    private static void addDates(JSONArray array, Set<LocalDate> target) throws Exception {
        if (array == null) {
            return;
        }
        for (int index = 0; index < array.length(); index++) {
            target.add(LocalDate.parse(array.getString(index)));
        }
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
