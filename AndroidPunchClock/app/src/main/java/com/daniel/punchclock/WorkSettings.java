package com.daniel.punchclock;

import android.content.Context;
import android.content.SharedPreferences;

final class WorkSettings {
    private static final String PREFS = "work_settings";
    private static final String REQUIRED_MINUTES = "required_minutes";
    private static final String DEDUCT_LUNCH = "deduct_lunch";
    private static final String LUNCH_MINUTES = "lunch_minutes";

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

    void setRequiredMinutes(int minutes) {
        prefs.edit().putInt(REQUIRED_MINUTES, clamp(minutes, 1, 16 * 60)).apply();
    }

    void setDeductLunch(boolean enabled) {
        prefs.edit().putBoolean(DEDUCT_LUNCH, enabled).apply();
    }

    void setLunchMinutes(int minutes) {
        prefs.edit().putInt(LUNCH_MINUTES, clamp(minutes, 0, 180)).apply();
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

    private String formatMinutes(int minutes) {
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
