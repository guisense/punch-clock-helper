package com.daniel.punchclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

final class BarChartView extends View {
    static final class Entry {
        final String label;
        final float value;
        final int color;
        final boolean showLabel;

        Entry(String label, float value, int color, boolean showLabel) {
            this.label = label;
            this.value = value;
            this.color = color;
            this.showLabel = showLabel;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Entry> entries = new ArrayList<>();
    private int mutedColor;
    private int textColor;
    private float maxValue = 1;

    BarChartView(Context context) {
        super(context);
        mutedColor = 0xFF94A3B8;
        textColor = 0xFF172033;
    }

    void setPalette(int mutedColor, int textColor) {
        this.mutedColor = mutedColor;
        this.textColor = textColor;
    }

    void setEntries(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        maxValue = 1;
        for (Entry entry : entries) {
            maxValue = Math.max(maxValue, entry.value);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0 || entries.isEmpty()) {
            return;
        }

        float labelHeight = dp(34);
        float valueHeight = dp(24);
        float chartTop = valueHeight;
        float chartBottom = height - labelHeight;
        float slot = width / (float) entries.size();
        float barWidth = Math.max(dp(7), Math.min(dp(30), slot * 0.52f));

        paint.setStrokeWidth(dp(1));
        paint.setColor(0xFFE2E8F0);
        canvas.drawLine(0, chartBottom, width, chartBottom, paint);

        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            float centerX = slot * index + slot / 2f;
            float normalized = entry.value / maxValue;
            float barHeight = Math.max(entry.value > 0 ? dp(7) : 0, normalized * (chartBottom - chartTop - dp(8)));
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;
            float top = chartBottom - barHeight;

            paint.setColor(entry.value > 0 ? entry.color : 0xFFE2E8F0);
            RectF rect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(rect, dp(5), dp(5), paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(11));
            paint.setColor(textColor);
            if (entry.value > 0) {
                float valueY = Math.max(dp(13), top - dp(6));
                canvas.drawText(valueText(entry.value), centerX, valueY, paint);
            }

            if (entry.showLabel) {
                paint.setColor(mutedColor);
                canvas.drawText(entry.label, centerX, height - dp(8), paint);
            }
        }
    }

    private String valueText(float value) {
        if (value >= 10 || Math.abs(value - Math.round(value)) < 0.08f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
