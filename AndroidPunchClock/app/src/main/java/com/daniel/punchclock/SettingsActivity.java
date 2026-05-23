package com.daniel.punchclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private WorkSettings settings;
    private TextView requiredValue;
    private TextView lunchValue;
    private TextView targetValue;
    private CheckBox lunchCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new WorkSettings(this);
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
        root.addView(workPanel);

        LinearLayout dataPanel = panel();
        dataPanel.addView(text("資料與隱私", 19, R.color.text, true));
        TextView privacy = text("所有打卡記錄與設定只保存在本機，不會上傳到服務器。卸載 App 可能會清除本機資料。", 15, R.color.muted, false);
        privacy.setPadding(0, dp(10), 0, 0);
        dataPanel.addView(privacy);
        root.addView(dataPanel);

        LinearLayout holidayPanel = panel();
        holidayPanel.addView(text("節假日規則", 19, R.color.text, true));
        TextView holiday = text("目前內建 2026 年中國法定節假日與調休日。後續會加入線上更新或手動導入。", 15, R.color.muted, false);
        holiday.setPadding(0, dp(10), 0, 0);
        holidayPanel.addView(holiday);
        root.addView(holidayPanel);

        setContentView(scrollView);
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
        lunchCheck.setChecked(settings.deductLunch());
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
