package com.daniel.punchclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class OnboardingActivity extends Activity {
    private WorkSettings settings;
    private int step = 0;
    private TextView title;
    private TextView subtitle;
    private TextView value;
    private CheckBox lunchCheck;
    private Button nextButton;
    private MinuteDialView dial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new WorkSettings(this);
        buildUi();
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        root.setBackgroundColor(color(R.color.surface));

        title = text("", 28, R.color.text, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        subtitle = text("", 15, R.color.muted, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(10), 0, dp(18));
        root.addView(subtitle);

        value = text("", 22, R.color.blue, true);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, 0, 0, dp(14));
        root.addView(value);

        dial = new MinuteDialView(this);
        dial.configure(settings.requiredMinutes(), 1, 16 * 60, this::updateCurrentStepValue);
        root.addView(dial, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(300)));

        lunchCheck = new CheckBox(this);
        lunchCheck.setText("扣除午休");
        lunchCheck.setTextSize(16);
        lunchCheck.setTextColor(color(R.color.text));
        lunchCheck.setOnCheckedChangeListener((buttonView, checked) -> settings.setDeductLunch(checked));
        root.addView(lunchCheck);

        nextButton = new Button(this);
        nextButton.setAllCaps(false);
        nextButton.setText("下一步");
        nextButton.setOnClickListener(view -> next());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        buttonParams.setMargins(0, dp(16), 0, 0);
        root.addView(nextButton, buttonParams);

        setContentView(root);
    }

    private void refresh() {
        lunchCheck.setVisibility(step == 1 ? android.view.View.VISIBLE : android.view.View.GONE);

        if (step == 0) {
            title.setText("每日工時");
            subtitle.setText("設定每天需要完成的工作時長。");
            value.setText(display(settings.requiredMinutes()));
            dial.configure(settings.requiredMinutes(), 1, 16 * 60, this::updateCurrentStepValue);
        } else if (step == 1) {
            title.setText("午休扣除");
            subtitle.setText("需要扣除午休時，安全下班會自動延後。");
            value.setText(display(settings.lunchMinutes()));
            lunchCheck.setChecked(settings.deductLunch());
            dial.configure(settings.lunchMinutes(), 0, 180, this::updateCurrentStepValue);
        } else {
            title.setText("安全緩衝");
            subtitle.setText("避免提前一分鐘造成缺勤。");
            value.setText(settings.safetyBufferText());
            dial.configure(settings.safetyBufferMinutes(), 0, 60, this::updateCurrentStepValue);
            nextButton.setText("完成");
        }
    }

    private void updateCurrentStepValue(int minutes) {
        if (step == 0) {
            settings.setRequiredMinutes(minutes);
            value.setText(display(settings.requiredMinutes()));
        } else if (step == 1) {
            settings.setLunchMinutes(minutes);
            value.setText(display(settings.lunchMinutes()));
        } else {
            settings.setSafetyBufferMinutes(minutes);
            value.setText(settings.safetyBufferText());
        }
    }

    private void next() {
        if (step < 2) {
            step++;
            refresh();
            return;
        }
        settings.setOnboardingCompleted(true);
        CountdownNotifier.update(this);
        finish();
    }

    private String display(int minutes) {
        if (minutes < 60) {
            return minutes + " 分鐘";
        }
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分鐘";
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
