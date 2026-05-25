package com.daniel.punchclock;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HistoryActivity extends Activity {
    private AttendanceStore store;
    private WorkSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AttendanceStore(this);
        settings = new WorkSettings(this);
        WorkdayPolicy.loadExternalRules(this);
        buildUi();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), UiStyle.topInset(this), dp(20), dp(20));
        root.setBackgroundColor(color(R.color.surface));
        scrollView.addView(root);

        LinearLayout header = row();
        TextView title = text("日歷", 28, R.color.text, true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button back = new Button(this);
        back.setText("返回");
        back.setAllCaps(false);
        back.setTextSize(14);
        UiStyle.styleSoftButton(back, this, R.color.blue, false);
        back.setOnClickListener(view -> finish());
        header.addView(back);
        root.addView(header);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate monthStart = today.minusDays(29);

        root.addView(summaryRow(monthStart, today));
        root.addView(chartSection("本自然週工時", "小時", weekStart, weekEnd, true, true));
        root.addView(chartSection("最近一月加班", "分鐘", monthStart, today, false, false));
        root.addView(detailSection(monthStart, today));

        setContentView(scrollView);
    }

    private LinearLayout summaryRow(LocalDate start, LocalDate end) {
        List<WorkRecord> records = store.recordsBetween(start, end);
        int workdays = 0;
        int recorded = 0;
        long overtime = 0;

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (WorkdayPolicy.isWorkday(cursor)) {
                workdays++;
            }
            cursor = cursor.plusDays(1);
        }

        for (WorkRecord record : records) {
            if (record.workedMinutes(settings) >= 0) {
                recorded++;
                overtime += record.overtimeMinutes(settings);
            }
        }

        LinearLayout row = row();
        row.setPadding(0, dp(12), 0, dp(6));
        row.addView(metric("工作日", String.valueOf(workdays)), metricParams());
        row.addView(metric("已記錄", String.valueOf(recorded)), metricParams());
        row.addView(metric("加班", overtime + " 分"), metricParams());
        return row;
    }

    private LinearLayout chartSection(String title, String unit, LocalDate start, LocalDate end, boolean showWorkedHours, boolean weekLabels) {
        LinearLayout section = panel();

        LinearLayout titleRow = row();
        titleRow.addView(text(title, 19, R.color.text, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleRow.addView(text(unit, 13, R.color.muted, false));
        section.addView(titleRow);

        BarChartView chart = new BarChartView(this);
        chart.setPalette(color(R.color.muted), color(R.color.text));
        chart.setEntries(chartEntries(start, end, showWorkedHours, weekLabels));
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(showWorkedHours ? 190 : 210)
        );
        chartParams.setMargins(0, dp(14), 0, 0);
        section.addView(chart, chartParams);
        return section;
    }

    private List<BarChartView.Entry> chartEntries(LocalDate start, LocalDate end, boolean showWorkedHours, boolean weekLabels) {
        Map<LocalDate, WorkRecord> byDay = new HashMap<>();
        for (WorkRecord record : store.recordsBetween(start, end)) {
            byDay.put(record.day, record);
        }

        ArrayList<BarChartView.Entry> entries = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (WorkdayPolicy.isWorkday(cursor)) {
                WorkRecord record = byDay.get(cursor);
                float value = 0;
                int colorId = R.color.line;
                if (record != null) {
                    if (showWorkedHours && record.workedMinutes(settings) >= 0) {
                        value = record.workedMinutes(settings) / 60f;
                        colorId = colorFor(record.level(settings));
                    } else if (!showWorkedHours && record.overtimeMinutes(settings) >= 0) {
                        value = record.overtimeMinutes(settings);
                        colorId = colorFor(record.level(settings));
                    }
                }

                entries.add(new BarChartView.Entry(axisLabel(cursor, weekLabels), value, color(colorId), shouldShowAxisLabel(cursor, weekLabels, entries.size())));
            }
            cursor = cursor.plusDays(1);
        }
        return entries;
    }

    private LinearLayout detailSection(LocalDate start, LocalDate end) {
        LinearLayout section = panel();
        section.addView(text("最近記錄", 19, R.color.text, true));

        List<WorkRecord> records = store.recordsBetween(start, end);
        if (records.isEmpty()) {
            TextView empty = text("還沒有記錄", 16, R.color.muted, false);
            empty.setPadding(0, dp(14), 0, 0);
            section.addView(empty);
            return section;
        }

        for (int index = records.size() - 1; index >= 0; index--) {
            WorkRecord record = records.get(index);
            if (!WorkdayPolicy.isWorkday(record.day)) {
                continue;
            }
            section.addView(recordRow(record));
        }
        return section;
    }

    private LinearLayout recordRow(WorkRecord record) {
        LinearLayout row = row();
        row.setPadding(0, dp(12), 0, dp(4));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(Formatters.date(record.day), 16, R.color.text, true));
        TextView time = text(Formatters.time(record.clockIn) + " - " + Formatters.time(record.clockOut), 13, R.color.muted, false);
        time.setPadding(0, dp(3), 0, 0);
        left.addView(time);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout right = new LinearLayout(this);
        right.setGravity(Gravity.END);
        right.setOrientation(LinearLayout.VERTICAL);
        TextView worked = text(Formatters.workedTime(record.workedMinutes(settings)), 15, R.color.text, true);
        worked.setGravity(Gravity.END);
        right.addView(worked);
        TextView overtime = text("+" + Math.max(0, record.overtimeMinutes(settings)) + " 分", 13, colorFor(record.level(settings)), true);
        overtime.setGravity(Gravity.END);
        right.addView(overtime);
        row.addView(right);

        return row;
    }

    private LinearLayout metric(String title, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(UiStyle.panel(this));
        box.addView(text(title, 12, R.color.muted, false));
        TextView valueView = text(value, 18, R.color.text, true);
        valueView.setPadding(0, dp(3), 0, 0);
        box.addView(valueView);
        return box;
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(UiStyle.panel(this));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(12), 0, 0);
        layout.setLayoutParams(params);
        return layout;
    }

    private GradientDrawable roundRect(int colorId) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(colorId));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), color(R.color.line));
        return drawable;
    }

    private String axisLabel(LocalDate date, boolean weekLabels) {
        if (weekLabels) {
            switch (date.getDayOfWeek()) {
                case MONDAY:
                    return "週一";
                case TUESDAY:
                    return "週二";
                case WEDNESDAY:
                    return "週三";
                case THURSDAY:
                    return "週四";
                case FRIDAY:
                    return "週五";
                case SATURDAY:
                    return "週六";
                default:
                    return "週日";
            }
        }
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    private boolean shouldShowAxisLabel(LocalDate date, boolean weekLabels, int index) {
        if (weekLabels) {
            return true;
        }
        return index == 0 || date.getDayOfMonth() == 1 || index % 4 == 0;
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
        return R.color.line;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
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

    private LinearLayout.LayoutParams metricParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private int color(int colorId) {
        return getResources().getColor(colorId, getTheme());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
