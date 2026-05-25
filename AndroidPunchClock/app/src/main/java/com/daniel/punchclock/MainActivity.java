package com.daniel.punchclock;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MainActivity extends Activity {
    private AttendanceStore store;
    private WorkSettings settings;
    private LinearLayout root;
    private TextView heroLabelText;
    private TextView mainTimeText;
    private TextView safeMessageText;
    private TextView actionHintText;
    private TextView clockInText;
    private TextView clockOutText;
    private TextView workedText;
    private Button punchButton;
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AttendanceStore(this);
        settings = new WorkSettings(this);
        store.ensureDemoData();
        ReminderScheduler.ensureNotificationChannel(this);
        requestNotificationPermission();
        prepareFeedback();
        buildUi();
        refresh();
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(18));
        root.setBackgroundColor(color(R.color.surface));
        scrollView.addView(root);

        LinearLayout header = row();
        TextView title = text("打卡助手", 28, R.color.text, true);
        header.addView(title, weightParams());

        ImageButton historyButton = iconButton(R.drawable.ic_calendar, "查看日歷");
        historyButton.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));
        header.addView(historyButton);

        ImageButton settingsButton = iconButton(R.drawable.ic_settings, "打開設定");
        settingsButton.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settingsButton);
        root.addView(header);

        TextView date = text(Formatters.fullDate(LocalDate.now()), 14, R.color.muted, false);
        date.setPadding(0, dp(2), 0, dp(18));
        root.addView(date);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(0, dp(4), 0, dp(22));
        root.addView(hero);

        heroLabelText = text("今日打卡", 14, R.color.muted, true);
        heroLabelText.setPadding(0, dp(8), 0, 0);
        hero.addView(heroLabelText);

        mainTimeText = text("--:--", 54, R.color.text, true);
        mainTimeText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mainTimeText.setGravity(Gravity.CENTER);
        mainTimeText.setPadding(0, dp(2), 0, 0);
        hero.addView(mainTimeText);

        safeMessageText = text("先記錄上班時間", 16, R.color.muted, false);
        safeMessageText.setGravity(Gravity.CENTER);
        safeMessageText.setPadding(0, dp(8), 0, dp(18));
        safeMessageText.setSingleLine(true);
        safeMessageText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        hero.addView(safeMessageText);

        punchButton = circleButton("上班");
        punchButton.setOnClickListener(view -> handlePunch());
        hero.addView(punchButton);

        actionHintText = text("刷臉後點一下", 14, R.color.muted, false);
        actionHintText.setGravity(Gravity.CENTER);
        actionHintText.setPadding(0, dp(10), 0, 0);
        hero.addView(actionHintText);

        LinearLayout detail = simpleSection();
        clockInText = addDetail(detail, "上班時間");
        clockOutText = addDetail(detail, "下班時間");
        workedText = addDetail(detail, "實際工時");
        root.addView(detail);

        LinearLayout utilityRow = row();
        utilityRow.setPadding(0, dp(4), 0, 0);

        Button manualButton = actionButton("手動補錄", R.color.blue, false);
        manualButton.setOnClickListener(view -> startActivity(new Intent(this, ManualEntryActivity.class)));
        utilityRow.addView(manualButton, utilityButtonParams(false));

        Button clearButton = actionButton("清除今天", R.color.red, true);
        clearButton.setOnClickListener(view -> confirmClearToday());
        utilityRow.addView(clearButton, utilityButtonParams(true));
        root.addView(utilityRow);

        setContentView(scrollView);
    }

    private void handlePunch() {
        WorkRecord record = store.todayRecord();
        LocalDateTime now = LocalDateTime.now();

        if (record.clockIn == null) {
            store.recordClockIn(now);
            ReminderScheduler.scheduleSafeClockOut(this, store.todayRecord(), settings);
            CountdownNotifier.update(this);
            playPunchFeedback();
        } else {
            store.recordClockOut(now);
            CountdownNotifier.cancel(this);
            playPunchFeedback();
        }

        refresh();
    }

    private void confirmClearToday() {
        new AlertDialog.Builder(this)
                .setTitle("清除今天？")
                .setMessage("今天的上班、下班記錄會被刪除，這個操作不能撤銷。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (dialog, which) -> {
                    store.clearToday();
                    ReminderScheduler.cancelToday(this);
                    CountdownNotifier.cancel(this);
                    refresh();
                })
                .show();
    }

    private void refresh() {
        if (store == null || mainTimeText == null) {
            return;
        }

        WorkRecord record = store.todayRecord();
        if (record.clockIn == null) {
            heroLabelText.setText("今日打卡");
            mainTimeText.setText("--:--");
            punchButton.setText("上班\n打卡");
            punchButton.setBackground(circleBackground(R.color.blue));
            actionHintText.setText("刷臉後點一下");
        } else if (record.clockOut == null) {
            heroLabelText.setText("上班打卡");
            mainTimeText.setText(Formatters.time(record.clockIn));
            punchButton.setText("下班\n打卡");
            punchButton.setBackground(circleBackground(R.color.green));
            actionHintText.setText("刷臉下班後點一下");
        } else {
            heroLabelText.setText("下班打卡");
            mainTimeText.setText(Formatters.time(record.clockOut));
            punchButton.setText("更新\n下班");
            punchButton.setBackground(circleBackground(R.color.orange));
            actionHintText.setText("已完成，點圓鈕可修正下班時間");
        }

        clockInText.setText(Formatters.time(record.clockIn));
        clockOutText.setText(Formatters.time(record.clockOut));
        workedText.setText(Formatters.workedTime(record.workedMinutes(settings)));

        setSafeMessage(record);
    }

    private void setSafeMessage(WorkRecord record) {
        LocalDateTime safe = record.safeClockOut(settings);
        if (safe == null) {
            safeMessageText.setText("打卡後顯示實際時間");
            safeMessageText.setTextColor(color(R.color.muted));
            return;
        }

        long remaining = Duration.between(LocalDateTime.now(), safe).toMinutes();
        if (remaining <= 0) {
            safeMessageText.setText("安全下班 " + Formatters.time(safe) + " · 可下班");
            safeMessageText.setTextColor(color(R.color.green));
            return;
        }

        safeMessageText.setText("安全下班 " + Formatters.time(safe) + " · 剩 " + Formatters.remainingMinutes(Math.max(1, remaining)));
        safeMessageText.setTextColor(color(R.color.text));
    }

    private void prepareFeedback() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55);
    }

    private void playPunchFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, 120));
            } else {
                vibrator.vibrate(60);
            }
        }

        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        }
    }

    private TextView addDetail(LinearLayout parent, String title) {
        LinearLayout row = row();
        row.setPadding(0, dp(11), 0, dp(11));
        row.addView(text(title, 16, R.color.muted, false), weightParams());
        TextView value = text("--:--", 17, R.color.text, true);
        row.addView(value);
        parent.addView(row);
        return value;
    }

    private LinearLayout simpleSection() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, dp(8));
        layout.setBackgroundColor(color(R.color.panel));
        LinearLayout.LayoutParams params = blockParams();
        params.setMargins(0, 0, 0, dp(12));
        layout.setLayoutParams(params);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView text(String value, int sp, int colorId, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color(colorId));
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private TextView badge(String value, int colorId) {
        TextView textView = text(value, 14, colorId, true);
        textView.setPadding(dp(10), dp(6), dp(10), dp(6));
        return textView;
    }

    private Button circleButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(24);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setBackground(circleBackground(R.color.blue));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(178), dp(178));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        button.setLayoutParams(params);
        return button;
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        return button;
    }

    private Button actionButton(String value, int colorId, boolean danger) {
        Button button = compactButton(value);
        button.setTextSize(15);
        button.setTextColor(danger ? color(colorId) : color(R.color.text));
        button.setBackground(outlineBackground(colorId, danger));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        return button;
    }

    private ImageButton iconButton(int iconId, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconId);
        button.setContentDescription(description);
        button.setBackgroundColor(0x00000000);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(44));
        params.setMargins(dp(6), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable circleBackground(int colorId) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color(colorId));
        return drawable;
    }

    private GradientDrawable outlineBackground(int colorId, boolean stronger) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(8));
        drawable.setColor(stronger ? 0xFFFFF1F4 : 0xFFFFFFFF);
        drawable.setStroke(dp(stronger ? 2 : 1), color(colorId));
        return drawable;
    }

    private int colorFor(WorkRecord.Level level) {
        if (level == WorkRecord.Level.GREEN) {
            return R.color.green;
        }
        if (level == WorkRecord.Level.ORANGE) {
            return R.color.orange;
        }
        if (level == WorkRecord.Level.RED) {
            return R.color.red;
        }
        return R.color.muted;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }

    }

    private LinearLayout.LayoutParams blockParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams utilityButtonParams(boolean trailing) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        if (trailing) {
            params.setMargins(dp(10), 0, 0, 0);
        }
        return params;
    }

    private int color(int colorId) {
        return getResources().getColor(colorId, getTheme());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
