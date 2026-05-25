package com.daniel.punchclock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class SettingsActivity extends Activity {
    private static final int EXPORT_CSV = 201;
    private static final int EXPORT_JSON = 202;
    private static final int IMPORT_JSON = 203;

    private WorkSettings settings;
    private AttendanceStore store;
    private TextView requiredValue;
    private TextView lunchValue;
    private TextView bufferValue;
    private TextView targetValue;
    private TextView notificationStatus;
    private TextView exactAlarmStatus;
    private CheckBox lunchCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new WorkSettings(this);
        store = new AttendanceStore(this);
        buildUi();
        refresh();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));
        root.setBackgroundColor(color(R.color.surface));
        scrollView.addView(root);

        LinearLayout header = row();
        TextView title = text("設定", 28, R.color.text, true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button back = compactButton("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back);
        root.addView(header);

        LinearLayout workPanel = panel();
        workPanel.addView(text("工時計算", 19, R.color.text, true));

        LinearLayout requiredRow = settingRow("每日工時");
        requiredValue = text("", 16, R.color.text, true);
        requiredRow.addView(requiredValue);
        requiredRow.setOnClickListener(view -> editMinutes("每日工時", settings.requiredMinutes(), 1, 16 * 60, value -> settings.setRequiredMinutes(value)));
        workPanel.addView(requiredRow);

        lunchCheck = new CheckBox(this);
        lunchCheck.setText("扣除午休");
        lunchCheck.setTextSize(16);
        lunchCheck.setTextColor(color(R.color.text));
        lunchCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setDeductLunch(isChecked);
            CountdownNotifier.update(this);
            refresh();
        });
        workPanel.addView(lunchCheck);

        LinearLayout lunchRow = settingRow("午休時長");
        lunchValue = text("", 16, R.color.text, true);
        lunchRow.addView(lunchValue);
        lunchRow.setOnClickListener(view -> editMinutes("午休時長", settings.lunchMinutes(), 0, 180, value -> settings.setLunchMinutes(value)));
        workPanel.addView(lunchRow);

        LinearLayout targetRow = settingRow("在公司時長");
        targetValue = text("", 16, R.color.blue, true);
        targetRow.addView(targetValue);
        workPanel.addView(targetRow);

        LinearLayout bufferRow = settingRow("安全緩衝");
        bufferValue = text("", 16, R.color.text, true);
        bufferRow.addView(bufferValue);
        bufferRow.setOnClickListener(view -> editMinutes("安全緩衝", settings.safetyBufferMinutes(), 0, 60, value -> settings.setSafetyBufferMinutes(value)));
        workPanel.addView(bufferRow);
        root.addView(workPanel);

        LinearLayout dataPanel = panel();
        dataPanel.addView(text("資料與隱私", 19, R.color.text, true));
        TextView privacy = text("所有打卡記錄與設定只保存在本機，不會上傳到服務器。卸載 App 可能會清除本機資料。", 15, R.color.muted, false);
        privacy.setPadding(0, dp(10), 0, 0);
        dataPanel.addView(privacy);
        LinearLayout exportRow = row();
        exportRow.setPadding(0, dp(12), 0, 0);
        Button csvButton = compactButton("匯出 CSV");
        csvButton.setOnClickListener(view -> createDocument(EXPORT_CSV, "text/csv", "punch-clock.csv"));
        exportRow.addView(csvButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button jsonButton = compactButton("備份 JSON");
        jsonButton.setOnClickListener(view -> createDocument(EXPORT_JSON, "application/json", "punch-clock-backup.json"));
        LinearLayout.LayoutParams jsonParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        jsonParams.setMargins(dp(10), 0, 0, 0);
        exportRow.addView(jsonButton, jsonParams);
        dataPanel.addView(exportRow);
        Button importButton = compactButton("恢復 JSON 備份");
        importButton.setOnClickListener(view -> openDocument());
        dataPanel.addView(importButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        root.addView(dataPanel);

        LinearLayout holidayPanel = panel();
        holidayPanel.addView(text("節假日規則", 19, R.color.text, true));
        TextView holiday = text("目前狀態：" + settings.holidayStatus() + "\n最後更新：" + settings.holidayUpdatedAt(), 15, R.color.muted, false);
        holiday.setPadding(0, dp(10), 0, 0);
        holidayPanel.addView(holiday);
        Button updateHoliday = compactButton("從 GitHub 更新節假日");
        updateHoliday.setOnClickListener(view -> updateHolidays());
        holidayPanel.addView(updateHoliday, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        root.addView(holidayPanel);

        LinearLayout reliabilityPanel = panel();
        reliabilityPanel.addView(text("提醒可靠性檢查", 19, R.color.text, true));
        notificationStatus = addStatusRow(reliabilityPanel, "通知權限");
        exactAlarmStatus = addStatusRow(reliabilityPanel, "鬧鐘提醒");
        TextView colorOs = text("ColorOS：建議允許通知、鬧鐘提醒、自啟動，並取消電池限制。", 15, R.color.muted, false);
        colorOs.setPadding(0, dp(10), 0, 0);
        reliabilityPanel.addView(colorOs);
        TextView widget = text("桌面小組件與常駐通知會在打卡、補錄、設定變更時刷新。", 15, R.color.muted, false);
        widget.setPadding(0, dp(8), 0, 0);
        reliabilityPanel.addView(widget);

        Button openNotification = compactButton("打開通知設定");
        openNotification.setOnClickListener(view -> openAppSettings());
        reliabilityPanel.addView(openNotification, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        root.addView(reliabilityPanel);

        setContentView(scrollView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            if (requestCode == EXPORT_CSV) {
                writeText(uri, BackupCodec.toCsv(store, settings));
            } else if (requestCode == EXPORT_JSON) {
                writeText(uri, BackupCodec.toJson(store, settings));
            } else if (requestCode == IMPORT_JSON) {
                confirmImport(readText(uri));
            }
        } catch (Exception error) {
            showMessage("操作失敗", error.getMessage());
        }
    }

    private void editMinutes(String title, int current, int min, int max, MinuteSetter setter) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(14), dp(4), dp(14), 0);

        TextView liveValue = text("", 16, R.color.blue, true);
        liveValue.setGravity(Gravity.CENTER);
        liveValue.setPadding(0, 0, 0, dp(6));

        MinuteDialView dial = new MinuteDialView(this);
        dial.configure(current, min, max, value -> liveValue.setText(displayMinutes(value)));
        liveValue.setText(displayMinutes(current));

        container.addView(liveValue);
        container.addView(dial, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(300)));

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("像時鐘一樣拖動圓環，範圍 " + min + " - " + max + " 分鐘。")
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    setter.set(dial.value());
                    CountdownNotifier.update(this);
                    refresh();
                })
                .show();
    }

    private LinearLayout settingRow(String title) {
        LinearLayout row = row();
        row.setPadding(0, dp(14), 0, dp(4));
        row.addView(text(title, 16, R.color.muted, false), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private interface MinuteSetter {
        void set(int value);
    }

    private String displayMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + " 分鐘";
        }
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
    }

    private void refresh() {
        requiredValue.setText(settings.requiredText());
        lunchValue.setText(settings.lunchText());
        targetValue.setText(settings.targetText());
        bufferValue.setText(settings.safetyBufferText());
        lunchCheck.setChecked(settings.deductLunch());
        refreshReliability();
    }

    private TextView addStatusRow(LinearLayout parent, String title) {
        LinearLayout row = settingRow(title);
        TextView value = text("", 16, R.color.text, true);
        row.addView(value);
        parent.addView(row);
        return value;
    }

    private void refreshReliability() {
        boolean notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        notificationStatus.setText(notifications ? "已允許" : "未允許");
        notificationStatus.setTextColor(color(notifications ? R.color.green : R.color.red));

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        boolean exactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
        exactAlarmStatus.setText(exactAlarm ? "可用" : "需允許");
        exactAlarmStatus.setTextColor(color(exactAlarm ? R.color.green : R.color.red));
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void createDocument(int requestCode, String mimeType, String name) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, requestCode);
    }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_JSON);
    }

    private void confirmImport(String json) {
        new AlertDialog.Builder(this)
                .setTitle("恢復備份？")
                .setMessage("這會覆蓋本機所有打卡記錄與設定。")
                .setNegativeButton("取消", null)
                .setPositiveButton("恢復", (dialog, which) -> {
                    try {
                        JSONObject root = new JSONObject(json);
                        settings.applyJson(root.optJSONObject("settings"));
                        store.replaceRecords(BackupCodec.recordsFromJson(root));
                        CountdownNotifier.update(this);
                        refresh();
                        showMessage("恢復完成", "記錄與設定已更新。");
                    } catch (Exception error) {
                        showMessage("恢復失敗", error.getMessage());
                    }
                })
                .show();
    }

    private void updateHolidays() {
        new Thread(() -> {
            String result = HolidayUpdater.updateFromGitHub(this);
            runOnUiThread(() -> {
                refresh();
                showMessage("節假日更新", result);
            });
        }).start();
    }

    private void writeText(Uri uri, String value) throws Exception {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) {
                throw new IllegalStateException("無法打開文件");
            }
            stream.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readText(Uri uri) throws Exception {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IllegalStateException("無法讀取文件");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        }
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message == null ? "" : message)
                .setPositiveButton("知道了", null)
                .show();
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackgroundColor(color(R.color.panel));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(14), 0, 0);
        layout.setLayoutParams(params);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }

    private TextView text(String value, int sp, int colorId, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color(colorId));
        if (bold) {
            textView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        }
        return textView;
    }

    private int color(int colorId) {
        return getResources().getColor(colorId, getTheme());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
