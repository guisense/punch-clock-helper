package com.daniel.punchclock;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

final class HolidayUpdater {
    private static final String HOLIDAY_URL = "https://raw.githubusercontent.com/guisense/punch-clock-helper/main/holidays/2026.json";

    private HolidayUpdater() {
    }

    static String updateFromGitHub(Context context) {
        WorkSettings settings = new WorkSettings(context);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(HOLIDAY_URL).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("HTTP " + code);
            }

            String raw = read(connection.getInputStream());
            JSONObject root = new JSONObject(raw);
            root.getJSONArray("holidays");
            root.getJSONArray("makeupWorkdays");
            WorkdayPolicy.saveExternalRules(context, raw);
            settings.setHolidayUpdateStatus(LocalDateTime.now().toString(), "GitHub 更新成功：" + root.optInt("year", 2026));
            return "已從 GitHub 更新節假日規則。";
        } catch (Exception error) {
            settings.setHolidayUpdateStatus(LocalDateTime.now().toString(), "GitHub 更新失敗，使用內建規則");
            return "更新失敗，已保留內建規則。\n" + error.getMessage();
        }
    }

    private static String read(InputStream stream) throws Exception {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        }
    }
}
