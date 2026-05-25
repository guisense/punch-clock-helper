package com.daniel.punchclock;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

final class HolidayUpdater {
    private static final String HOLIDAY_URL = "https://raw.githubusercontent.com/guisense/punch-clock-helper/main/holidays/%s/2026.json";

    private HolidayUpdater() {
    }

    static String updateFromGitHub(Context context) {
        WorkSettings settings = new WorkSettings(context);
        String region = settings.region();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(HOLIDAY_URL, region)).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("HTTP " + code);
            }

            String raw = read(connection.getInputStream());
            JSONObject root = new JSONObject(raw);
            String remoteRegion = WorkdayPolicy.normalizeRegion(root.optString("region", region));
            if (!remoteRegion.equals(region)) {
                throw new IllegalStateException("節假日資料地區不一致");
            }
            root.getJSONArray("holidays");
            root.getJSONArray("makeupWorkdays");
            WorkdayPolicy.saveExternalRules(context, region, raw);
            settings.setHolidayUpdateStatus(LocalDateTime.now().toString(), settings.regionLabel() + " GitHub 更新成功：" + root.optInt("year", 2026));
            return "已從 GitHub 更新" + settings.regionLabel() + "節假日規則。";
        } catch (Exception error) {
            settings.setHolidayUpdateStatus(LocalDateTime.now().toString(), settings.regionLabel() + " GitHub 更新失敗，使用內建規則");
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
