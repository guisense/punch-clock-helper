package com.daniel.punchclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

public final class MinuteDialView extends View {
    interface OnValueChanged {
        void onChanged(int value);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int value;
    private int min;
    private int max;
    private double lastAngle;
    private int lastHapticBucket = -1;
    private OnValueChanged listener;

    public MinuteDialView(Context context) {
        super(context);
        setMinimumHeight(dp(260));
    }

    void configure(int value, int min, int max, OnValueChanged listener) {
        this.value = clamp(value, min, max);
        this.min = min;
        this.max = max;
        this.listener = listener;
        invalidate();
    }

    int value() {
        return value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) * 0.36f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(18));
        paint.setColor(0xFFE8ECF5);
        canvas.drawCircle(cx, cy, radius, paint);

        float sweep = ((value % 60) / 60f) * 360f;
        if (sweep == 0 && value > 0) {
            sweep = 360f;
        }
        paint.setColor(0xFF7BA7FF);
        RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(rect, -90, sweep, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, radius - dp(24), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xFF253047);
        paint.setTextSize(dp(34));
        paint.setFakeBoldText(true);
        canvas.drawText(formatValue(value), cx, cy - dp(2), paint);

        paint.setFakeBoldText(false);
        paint.setColor(0xFF7A869A);
        paint.setTextSize(dp(13));
        canvas.drawText("拖動圓環調整，最小 1 分鐘", cx, cy + dp(28), paint);

        drawTicks(canvas, cx, cy, radius);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        double angle = angleFor(event.getX(), event.getY());

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastAngle = angle;
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            double delta = angle - lastAngle;
            if (delta > 180) {
                delta -= 360;
            } else if (delta < -180) {
                delta += 360;
            }

            int change = (int) Math.round(delta / 6.0);
            if (change != 0) {
                value = clamp(value + change, min, max);
                lastAngle = angle;
                performDialHaptic(value);
                if (listener != null) {
                    listener.onChanged(value);
                }
                invalidate();
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void drawTicks(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int index = 0; index < 12; index++) {
            double radians = Math.toRadians(index * 30 - 90);
            float inner = radius + dp(16);
            float outer = radius + dp(index % 3 == 0 ? 28 : 22);
            paint.setStrokeWidth(dp(index % 3 == 0 ? 3 : 2));
            paint.setColor(index % 3 == 0 ? 0xFFC5B8FF : 0xFFDDE3F0);
            canvas.drawLine(
                    cx + (float) Math.cos(radians) * inner,
                    cy + (float) Math.sin(radians) * inner,
                    cx + (float) Math.cos(radians) * outer,
                    cy + (float) Math.sin(radians) * outer,
                    paint
            );
        }
    }

    private double angleFor(float x, float y) {
        double angle = Math.toDegrees(Math.atan2(y - getHeight() / 2f, x - getWidth() / 2f)) + 90;
        return angle < 0 ? angle + 360 : angle;
    }

    private void performDialHaptic(int currentValue) {
        int bucket = currentValue / 5;
        if (bucket == lastHapticBucket) {
            return;
        }
        lastHapticBucket = bucket;

        if (currentValue % 30 == 0) {
            vibrate(24, 95);
        } else {
            vibrate(8, 45);
        }
    }

    private void vibrate(long durationMs, int amplitude) {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    private String formatValue(int minutes) {
        if (minutes < 60) {
            return minutes + " 分鐘";
        }
        return (minutes / 60) + " 小時 " + (minutes % 60) + " 分";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
