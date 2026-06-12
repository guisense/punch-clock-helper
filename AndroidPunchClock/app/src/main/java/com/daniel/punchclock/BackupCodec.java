package com.daniel.punchclock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class BackupCodec {
    private BackupCodec() {
    }

    static String toJson(AttendanceStore store, WorkSettings settings) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("exportedAt", LocalDateTime.now().toString());
        root.put("settings", settings.toJson());

        JSONArray records = new JSONArray();
        for (WorkRecord record : store.allRecords()) {
            JSONObject object = new JSONObject();
            object.put("day", record.day.toString());
            if (record.clockIn != null) {
                object.put("clockIn", record.clockIn.toString());
            }
            if (record.clockOut != null) {
                object.put("clockOut", record.clockOut.toString());
            }
            if (record.leaveMinutes > 0) {
                object.put("leaveMinutes", record.leaveMinutes);
            }
            if (record.leaveStartMinutes != null && record.leaveEndMinutes != null) {
                object.put("leaveStartMinutes", record.leaveStartMinutes);
                object.put("leaveEndMinutes", record.leaveEndMinutes);
                object.put("leaveIncludesLunch", record.leaveIncludesLunch);
            }
            if (record.billableStartOverrideMinutes != null) {
                object.put("billableStartOverrideMinutes", record.billableStartOverrideMinutes);
            }
            if (record.deductLunchOverride != null) {
                object.put("deductLunchOverride", record.deductLunchOverride);
            }
            records.put(object);
        }
        root.put("records", records);
        return root.toString(2);
    }

    static List<WorkRecord> recordsFromJson(JSONObject root) throws JSONException {
        ArrayList<WorkRecord> records = new ArrayList<>();
        JSONArray array = root.optJSONArray("records");
        if (array == null) {
            return records;
        }
        for (int index = 0; index < array.length(); index++) {
            JSONObject object = array.getJSONObject(index);
            WorkRecord record = new WorkRecord(LocalDate.parse(object.getString("day")));
            if (object.has("clockIn")) {
                record.clockIn = LocalDateTime.parse(object.getString("clockIn"));
            }
            if (object.has("clockOut")) {
                record.clockOut = LocalDateTime.parse(object.getString("clockOut"));
            }
            record.leaveMinutes = Math.max(0, object.optInt("leaveMinutes", 0));
            if (object.has("leaveStartMinutes") && object.has("leaveEndMinutes")) {
                record.leaveStartMinutes = Math.max(0, Math.min(23 * 60 + 59, object.optInt("leaveStartMinutes", 0)));
                record.leaveEndMinutes = Math.max(0, Math.min(24 * 60, object.optInt("leaveEndMinutes", 0)));
                record.leaveIncludesLunch = object.optBoolean("leaveIncludesLunch", true);
            }
            if (object.has("billableStartOverrideMinutes")) {
                int minutes = object.optInt("billableStartOverrideMinutes", 0);
                record.billableStartOverrideMinutes = Math.max(0, Math.min(23 * 60 + 59, minutes));
            }
            if (object.has("deductLunchOverride")) {
                record.deductLunchOverride = object.optBoolean("deductLunchOverride");
            }
            records.add(record);
        }
        return records;
    }

    static String toCsv(AttendanceStore store, WorkSettings settings) {
        StringBuilder builder = new StringBuilder();
        builder.append("日期,上班時間,下班時間,請假時段,請假分鐘,實際工時分鐘,加班分鐘\n");
        for (WorkRecord record : store.sortedRecords()) {
            builder.append(record.day).append(',')
                    .append(Formatters.time(record.clockIn)).append(',')
                    .append(Formatters.time(record.clockOut)).append(',')
                    .append(record.leaveText(settings)).append(',')
                    .append(record.leaveCreditMinutes(settings)).append(',')
                    .append(Math.max(0, record.workedMinutes(settings))).append(',')
                    .append(Math.max(0, record.overtimeMinutes(settings))).append('\n');
        }
        return builder.toString();
    }
}
