package com.daniel.punchclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AttendanceStore {
    private static final String PREFS = "attendance_store";
    private static final String RECORDS = "records";
    private static final String DEMO_VERSION = "demo_version";
    private static final String LEGACY_DEMO_CLEANED = "legacy_demo_cleaned";
    private static final String LEGACY_DEMO_CLEANED_V2 = "legacy_demo_cleaned_v2";
    private static final String LEGACY_DEMO_CLEANED_V3 = "legacy_demo_cleaned_v3";

    private final SharedPreferences prefs;
    private final List<WorkRecord> records = new ArrayList<>();

    AttendanceStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
        removeLegacyDemoData();
    }

    void ensureDemoData() {
        if (prefs.getInt(DEMO_VERSION, 0) >= 2) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (int offset = 1; offset <= 30; offset++) {
            LocalDate day = today.minusDays(offset);
            if (!WorkdayPolicy.isWorkday(day)) {
                continue;
            }

            int hour = offset % 3 == 0 ? 7 : 8;
            int minute = new int[]{42, 55, 18, 48, 5, 33, 50}[offset % 7];
            int overtime = new int[]{0, 6, 10, 18, 24, 33, 47, 2, 29}[offset % 9];
            addDemoRecord(day, hour, minute, overtime);
        }
        prefs.edit().putInt(DEMO_VERSION, 2).apply();
        save();
    }

    WorkRecord todayRecord() {
        return recordFor(LocalDate.now());
    }

    List<WorkRecord> sortedRecords() {
        ArrayList<WorkRecord> copy = new ArrayList<>(records);
        copy.sort(Comparator.comparing((WorkRecord record) -> record.day).reversed());
        return copy;
    }

    List<WorkRecord> allRecords() {
        return new ArrayList<>(records);
    }

    List<WorkRecord> recordsBetween(LocalDate start, LocalDate end) {
        ArrayList<WorkRecord> copy = new ArrayList<>();
        for (WorkRecord record : records) {
            if ((record.day.isEqual(start) || record.day.isAfter(start))
                    && (record.day.isEqual(end) || record.day.isBefore(end))) {
                copy.add(record);
            }
        }
        copy.sort(Comparator.comparing(record -> record.day));
        return copy;
    }

    void recordClockIn(LocalDateTime time) {
        WorkRecord record = recordFor(time.toLocalDate());
        record.clockIn = time;
        if (record.clockOut != null && record.clockOut.isBefore(time)) {
            record.clockOut = null;
        }
        save();
    }

    void recordClockOut(LocalDateTime time) {
        WorkRecord record = recordFor(time.toLocalDate());
        record.clockOut = time;
        save();
    }

    void saveManualRecord(LocalDate day, LocalDateTime clockIn, LocalDateTime clockOut) {
        WorkRecord record = recordFor(day);
        record.clockIn = clockIn;
        record.clockOut = clockOut;
        save();
    }

    void clearClockIn(LocalDate day) {
        records.removeIf(record -> record.day.equals(day));
        save();
    }

    void clearClockOut(LocalDate day) {
        WorkRecord record = recordFor(day);
        record.clockOut = null;
        save();
    }

    void replaceRecords(List<WorkRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        save();
    }

    void clearToday() {
        LocalDate today = LocalDate.now();
        records.removeIf(record -> record.day.equals(today));
        save();
    }

    private WorkRecord recordFor(LocalDate day) {
        for (WorkRecord record : records) {
            if (record.day.equals(day)) {
                return record;
            }
        }
        WorkRecord record = new WorkRecord(day);
        records.add(record);
        return record;
    }

    private void addDemoRecord(LocalDate day, int hour, int minute, int overtimeMinutes) {
        for (WorkRecord existing : records) {
            if (existing.day.equals(day)) {
                return;
            }
        }

        WorkRecord record = new WorkRecord(day);
        record.clockIn = day.atTime(hour, minute);
        record.clockOut = record.clockIn.plusHours(8).plusMinutes(overtimeMinutes);
        records.add(record);
    }

    private void removeLegacyDemoData() {
        if (prefs.getBoolean(LEGACY_DEMO_CLEANED_V3, false) || prefs.getInt(DEMO_VERSION, 0) <= 0) {
            return;
        }

        boolean removed = false;
        LocalDate today = LocalDate.now();
        for (int seedOffset = 0; seedOffset <= 90; seedOffset++) {
            LocalDate seedDay = today.minusDays(seedOffset);
            for (int offset = 1; offset <= 30; offset++) {
                LocalDate day = seedDay.minusDays(offset);
                if (!WorkdayPolicy.isWorkday(day)) {
                    continue;
                }

                int hour = offset % 3 == 0 ? 7 : 8;
                int minute = new int[]{42, 55, 18, 48, 5, 33, 50}[offset % 7];
                int overtime = new int[]{0, 6, 10, 18, 24, 33, 47, 2, 29}[offset % 9];
                LocalDateTime demoClockIn = day.atTime(hour, minute);
                LocalDateTime demoClockOut = demoClockIn.plusHours(8).plusMinutes(overtime);
                removed |= records.removeIf(record -> day.equals(record.day)
                        && demoClockIn.equals(record.clockIn)
                        && demoClockOut.equals(record.clockOut));
            }
        }
        removed |= records.removeIf(record -> isMinutePrecisionLegacyDemo(record, today));

        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(LEGACY_DEMO_CLEANED, true)
                .putBoolean(LEGACY_DEMO_CLEANED_V2, true)
                .putBoolean(LEGACY_DEMO_CLEANED_V3, true);
        if (removed) {
            JSONArray array = new JSONArray();
            for (WorkRecord record : records) {
                JSONObject object = new JSONObject();
                try {
                    object.put("day", record.day.toString());
                    if (record.clockIn != null) {
                        object.put("clockIn", record.clockIn.toString());
                    }
                    if (record.clockOut != null) {
                        object.put("clockOut", record.clockOut.toString());
                    }
                    array.put(object);
                } catch (JSONException ignored) {
                }
            }
            editor.putString(RECORDS, array.toString());
        }
        editor.apply();
    }

    private boolean isMinutePrecisionLegacyDemo(WorkRecord record, LocalDate today) {
        if (record.clockIn == null || record.clockOut == null) {
            return false;
        }
        if (record.day.isEqual(today) || record.day.isBefore(today.minusDays(120))) {
            return false;
        }
        return record.clockIn.getSecond() == 0
                && record.clockIn.getNano() == 0
                && record.clockOut.getSecond() == 0
                && record.clockOut.getNano() == 0;
    }

    private void load() {
        records.clear();
        String raw = prefs.getString(RECORDS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                JSONObject object = array.getJSONObject(index);
                WorkRecord record = new WorkRecord(LocalDate.parse(object.getString("day")));
                if (object.has("clockIn")) {
                    record.clockIn = LocalDateTime.parse(object.getString("clockIn"));
                }
                if (object.has("clockOut")) {
                    record.clockOut = LocalDateTime.parse(object.getString("clockOut"));
                }
                records.add(record);
            }
        } catch (JSONException ignored) {
            records.clear();
        }
    }

    private void save() {
        JSONArray array = new JSONArray();
        for (WorkRecord record : records) {
            JSONObject object = new JSONObject();
            try {
                object.put("day", record.day.toString());
                if (record.clockIn != null) {
                    object.put("clockIn", record.clockIn.toString());
                }
                if (record.clockOut != null) {
                    object.put("clockOut", record.clockOut.toString());
                }
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(RECORDS, array.toString()).apply();
    }
}
