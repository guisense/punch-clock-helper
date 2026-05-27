package com.daniel.punchclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;

final class WorkSettings {
    private static final String PREFS = "work_settings";
    private static final String REQUIRED_MINUTES = "required_minutes";
    private static final String DEDUCT_LUNCH = "deduct_lunch";
    private static final String LUNCH_MINUTES = "lunch_minutes";
    private static final String SAFETY_BUFFER_MINUTES = "safety_buffer_minutes";
    private static final String EARLIEST_BILLABLE_START_MINUTES = "earliest_billable_start_minutes";
    private static final String ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String HOLIDAY_UPDATED_AT = "holiday_updated_at";
    private static final String HOLIDAY_STATUS = "holiday_status";
    private static final String REGION = "region";

    private final SharedPreferences prefs;

    WorkSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    int requiredMinutes() {
        return prefs.getInt(REQUIRED_MINUTES, 8 * 60);
    }

    boolean deductLunch() {
        return prefs.getBoolean(DEDUCT_LUNCH, false);
    }

    int lunchMinutes() {
        return prefs.getInt(LUNCH_MINUTES, 60);
    }

    int targetPresenceMinutes() {
        return requiredMinutes() + (deductLunch() ? lunchMinutes() : 0);
    }

    int safetyBufferMinutes() {
        return prefs.getInt(SAFETY_BUFFER_MINUTES, 2);
    }

    int earliestBillableStartMinutes() {
        return prefs.getInt(EARLIEST_BILLABLE_START_MINUTES, 7 * 60 + 30);
    }

    LocalTime earliestBillableStartTime() {
        return LocalTime.of(earliestBillableStartMinutes() / 60, earliestBillableStartMinutes() % 60);
    }

    boolean onboardingCompleted() {
        return prefs.getBoolean(ONBOARDING_COMPLETED, false);
    }

    String holidayUpdatedAt() {
        return prefs.getString(HOLIDAY_UPDATED_AT, "尚未更新");
    }

    String holidayStatus() {
        return prefs.getString(HOLIDAY_STATUS, "使用內建規則");
    }

    String region() {
        return prefs.getString(REGION, WorkdayPolicy.REGION_CN);
    }

    String regionLabel() {
        return WorkdayPolicy.regionLabel(region());
    }

    void setRequiredMinutes(int minutes) {
        prefs.edit().putInt(REQUIRED_MINUTES, clamp(minutes, 1, 16 * 60)).apply();
    }

    void setDeductLunch(boolean enabled) {
        prefs.edit().putBoolean(DEDUCT_LUNCH, enabled).apply();
    }

    void setLunchMinutes(int minutes) {
        prefs.edit().putInt(LUNCH_MINUTES, clamp(minutes, 0, 180)).apply();
    }

    void setSafetyBufferMinutes(int minutes) {
        prefs.edit().putInt(SAFETY_BUFFER_MINUTES, clamp(minutes, 0, 60)).apply();
    }

    void setEarliestBillableStartMinutes(int minutes) {
        prefs.edit().putInt(EARLIEST_BILLABLE_START_MINUTES, clamp(minutes, 0, 23 * 60 + 59)).apply();
    }

    void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(ONBOARDING_COMPLETED, completed).apply();
    }

    void setHolidayUpdateStatus(String updatedAt, String status) {
        prefs.edit()
                .putString(HOLIDAY_UPDATED_AT, updatedAt)
                .putString(HOLIDAY_STATUS, status)
                .apply();
    }

    void setRegion(String region) {
        prefs.edit().putString(REGION, WorkdayPolicy.normalizeRegion(region)).apply();
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("requiredMinutes", requiredMinutes());
        object.put("deductLunch", deductLunch());
        object.put("lunchMinutes", lunchMinutes());
        object.put("safetyBufferMinutes", safetyBufferMinutes());
        object.put("earliestBillableStartMinutes", earliestBillableStartMinutes());
        object.put("onboardingCompleted", onboardingCompleted());
        object.put("region", region());
        return object;
    }

    void applyJson(JSONObject object) {
        if (object == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        if (object.has("requiredMinutes")) {
            editor.putInt(REQUIRED_MINUTES, clamp(object.optInt("requiredMinutes", requiredMinutes()), 1, 16 * 60));
        }
        if (object.has("deductLunch")) {
            editor.putBoolean(DEDUCT_LUNCH, object.optBoolean("deductLunch", deductLunch()));
        }
        if (object.has("lunchMinutes")) {
            editor.putInt(LUNCH_MINUTES, clamp(object.optInt("lunchMinutes", lunchMinutes()), 0, 180));
        }
        if (object.has("safetyBufferMinutes")) {
            editor.putInt(SAFETY_BUFFER_MINUTES, clamp(object.optInt("safetyBufferMinutes", safetyBufferMinutes()), 0, 60));
        }
        if (object.has("earliestBillableStartMinutes")) {
            editor.putInt(EARLIEST_BILLABLE_START_MINUTES, clamp(object.optInt("earliestBillableStartMinutes", earliestBillableStartMinutes()), 0, 23 * 60 + 59));
        }
        if (object.has("onboardingCompleted")) {
            editor.putBoolean(ONBOARDING_COMPLETED, object.optBoolean("onboardingCompleted", onboardingCompleted()));
        }
        if (object.has("region")) {
            editor.putString(REGION, WorkdayPolicy.normalizeRegion(object.optString("region", region())));
        }
        editor.apply();
    }

    String requiredText() {
        return formatMinutes(requiredMinutes());
    }

    String targetText() {
        return formatMinutes(targetPresenceMinutes());
    }

    String lunchText() {
        return lunchMinutes() + " 分鐘";
    }

    String safetyBufferText() {
        return safetyBufferMinutes() + " 分鐘";
    }

    String earliestBillableStartText() {
        return formatClockTime(earliestBillableStartMinutes());
    }

    private String formatMinutes(int minutes) {
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
    }

    private String formatClockTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
