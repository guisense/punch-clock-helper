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
    static final String REGION_CN = "cn";
    static final String REGION_HK = "hk";
    static final String REGION_TW = "tw";
    static final String[] REGIONS = {REGION_CN, REGION_HK, REGION_TW};
    static final String[] REGION_LABELS = {"中國大陸", "中國香港", "中國臺灣"};

    private static final String PREFS = "holiday_rules";
    private static final String RULES_JSON_PREFIX = "rules_json_";
    private static final Set<LocalDate> CN_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> CN_MAKEUP_WORKDAYS = new HashSet<>();
    private static final Set<LocalDate> HK_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> TW_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> EXTERNAL_HOLIDAYS = new HashSet<>();
    private static final Set<LocalDate> EXTERNAL_MAKEUP_WORKDAYS = new HashSet<>();
    private static String loadedExternalRegion = "";

    static {
        addRange(CN_HOLIDAYS, "2026-01-01", "2026-01-03");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-01-04");

        addRange(CN_HOLIDAYS, "2026-02-15", "2026-02-23");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-02-14");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-02-28");

        addRange(CN_HOLIDAYS, "2026-04-04", "2026-04-06");

        addRange(CN_HOLIDAYS, "2026-05-01", "2026-05-05");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-05-09");

        addRange(CN_HOLIDAYS, "2026-06-19", "2026-06-21");
        addRange(CN_HOLIDAYS, "2026-09-25", "2026-09-27");

        addRange(CN_HOLIDAYS, "2026-10-01", "2026-10-07");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-09-20");
        addWorkday(CN_MAKEUP_WORKDAYS, "2026-10-10");

        addDates(HK_HOLIDAYS, "2026-01-01", "2026-02-17", "2026-02-18", "2026-02-19",
                "2026-04-03", "2026-04-04", "2026-04-06", "2026-04-07",
                "2026-05-01", "2026-05-25", "2026-06-19", "2026-07-01",
                "2026-09-26", "2026-10-01", "2026-10-19", "2026-12-25", "2026-12-26");

        addDates(TW_HOLIDAYS, "2026-01-01", "2026-02-16", "2026-02-17", "2026-02-18",
                "2026-02-19", "2026-02-20", "2026-02-27", "2026-04-03",
                "2026-04-06", "2026-05-01", "2026-06-19", "2026-09-25",
                "2026-09-28", "2026-10-09", "2026-10-26", "2026-12-25");
    }

    private WorkdayPolicy() {
    }

    static boolean isWorkday(LocalDate date) {
        return isWorkday(date, REGION_CN);
    }

    static boolean isWorkday(LocalDate date, String region) {
        String normalized = normalizeRegion(region);
        if (EXTERNAL_HOLIDAYS.contains(date)) {
            return false;
        }
        if (EXTERNAL_MAKEUP_WORKDAYS.contains(date)) {
            return true;
        }
        if (holidaysFor(normalized).contains(date)) {
            return false;
        }
        if (makeupWorkdaysFor(normalized).contains(date)) {
            return true;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    static void loadExternalRules(Context context) {
        loadExternalRules(context, new WorkSettings(context).region());
    }

    static void loadExternalRules(Context context, String region) {
        String normalized = normalizeRegion(region);
        if (normalized.equals(loadedExternalRegion)) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(RULES_JSON_PREFIX + normalized, null);
        EXTERNAL_HOLIDAYS.clear();
        EXTERNAL_MAKEUP_WORKDAYS.clear();
        loadedExternalRegion = normalized;
        if (raw == null) {
            return;
        }
        applyExternalRules(raw);
    }

    static void saveExternalRules(Context context, String region, String raw) {
        String normalized = normalizeRegion(region);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(RULES_JSON_PREFIX + normalized, raw)
                .apply();
        loadedExternalRegion = normalized;
        applyExternalRules(raw);
    }

    static String normalizeRegion(String region) {
        if (REGION_HK.equals(region) || REGION_TW.equals(region)) {
            return region;
        }
        return REGION_CN;
    }

    static String regionLabel(String region) {
        String normalized = normalizeRegion(region);
        if (REGION_HK.equals(normalized)) {
            return "中國香港";
        }
        if (REGION_TW.equals(normalized)) {
            return "中國臺灣";
        }
        return "中國大陸";
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

    private static Set<LocalDate> holidaysFor(String region) {
        if (REGION_HK.equals(region)) {
            return HK_HOLIDAYS;
        }
        if (REGION_TW.equals(region)) {
            return TW_HOLIDAYS;
        }
        return CN_HOLIDAYS;
    }

    private static Set<LocalDate> makeupWorkdaysFor(String region) {
        if (REGION_CN.equals(region)) {
            return CN_MAKEUP_WORKDAYS;
        }
        return new HashSet<>();
    }

    private static void addRange(Set<LocalDate> target, String start, String end) {
        LocalDate current = LocalDate.parse(start);
        LocalDate last = LocalDate.parse(end);
        while (!current.isAfter(last)) {
            target.add(current);
            current = current.plusDays(1);
        }
    }

    private static void addWorkday(Set<LocalDate> target, String date) {
        target.add(LocalDate.parse(date));
    }

    private static void addDates(Set<LocalDate> target, String... dates) {
        for (String date : dates) {
            target.add(LocalDate.parse(date));
        }
    }
}
