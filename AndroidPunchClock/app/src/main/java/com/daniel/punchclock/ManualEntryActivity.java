package com.daniel.punchclock;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class ManualEntryActivity extends Activity {
    private AttendanceStore store;
    private LocalDate day;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private TextView dayValue;
    private TextView clockInValue;
    private TextView clockOutValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AttendanceStore(this);
        WorkRecord today = store.todayRecord();
        day = today.day;
        clockIn = today.clockIn == null ? LocalTime.of(8, 30) : today.clockIn.toLocalTime();
        clockOut = today.clockOut == null ? clockIn.plusHours(8) : today.clockOut.toLocalTime();
        buildUi();
        refresh();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), UiStyle.topInset(this), dp(20), dp(24));
        root.setBackgroundColor(color(R.color.surface));
        scrollView.addView(root);

        LinearLayout header = row();
        header.addView(text("手動補錄", 28, R.color.text, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button back = compactButton("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back);
        root.addView(header);

        LinearLayout panel = panel();
        dayValue = addPickRow(panel, "日期", view -> pickDate());
        clockInValue = addPickRow(panel, "上班時間", view -> pickTime(true));
        clockOutValue = addPickRow(panel, "下班時間", view -> pickTime(false));
        root.addView(panel);

        Button save = compactButton("保存補錄");
        save.setTextSize(18);
        UiStyle.stylePrimaryButton(save, this, R.color.blue);
        save.setOnClickListener(view -> {
            store.saveManualRecord(day, LocalDateTime.of(day, clockIn), LocalDateTime.of(day, clockOut));
            CountdownNotifier.update(this);
            finish();
        });
        root.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        setContentView(scrollView);
    }

    private TextView addPickRow(LinearLayout parent, String title, android.view.View.OnClickListener listener) {
        LinearLayout row = row();
        row.setPadding(0, dp(14), 0, dp(14));
        row.addView(text(title, 16, R.color.muted, false), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text("", 17, R.color.text, true);
        row.addView(value);
        row.setOnClickListener(listener);
        parent.addView(row);
        return value;
    }

    private void pickDate() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, date) -> {
            day = LocalDate.of(year, month + 1, date);
            WorkRecord record = store.recordsBetween(day, day).isEmpty() ? null : store.recordsBetween(day, day).get(0);
            if (record != null) {
                if (record.clockIn != null) {
                    clockIn = record.clockIn.toLocalTime();
                }
                if (record.clockOut != null) {
                    clockOut = record.clockOut.toLocalTime();
                }
            }
            refresh();
        }, day.getYear(), day.getMonthValue() - 1, day.getDayOfMonth());
        dialog.show();
    }

    private void pickTime(boolean isClockIn) {
        LocalTime current = isClockIn ? clockIn : clockOut;
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            if (isClockIn) {
                clockIn = LocalTime.of(hour, minute);
                if (!clockOut.isAfter(clockIn)) {
                    clockOut = clockIn.plusHours(8);
                }
            } else {
                clockOut = LocalTime.of(hour, minute);
            }
            refresh();
        }, current.getHour(), current.getMinute(), true);
        dialog.show();
    }

    private void refresh() {
        dayValue.setText(day.toString());
        clockInValue.setText(Formatters.time(clockIn));
        clockOutValue.setText(Formatters.time(clockOut));
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));
        layout.setBackground(UiStyle.panel(this));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(14), 0, dp(18));
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
        button.setTextSize(14);
        UiStyle.styleSoftButton(button, this, R.color.blue, false);
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
