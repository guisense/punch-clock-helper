package com.daniel.punchclock;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;

final class UiStyle {
    private UiStyle() {
    }

    static GradientDrawable panel(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color(context, R.color.panel));
        drawable.setCornerRadius(dp(context, 18));
        drawable.setStroke(dp(context, 1), color(context, R.color.line));
        return drawable;
    }

    static GradientDrawable softCard(Context context) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{color(context, R.color.widget_card_start), color(context, R.color.widget_card_end)}
        );
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, 26));
        drawable.setStroke(dp(context, 1), color(context, R.color.widget_card_stroke));
        return drawable;
    }

    static GradientDrawable pill(Context context, int colorId) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color(context, colorId));
        drawable.setCornerRadius(dp(context, 999));
        return drawable;
    }

    static GradientDrawable outline(Context context, int colorId, boolean softFill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, 16));
        drawable.setColor(softFill ? softFillColor(color(context, colorId)) : color(context, R.color.panel));
        drawable.setStroke(dp(context, 1), color(context, colorId));
        return drawable;
    }

    static GradientDrawable circle(Context context, int colorId) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{color(context, colorId), mix(color(context, colorId), 0xFFFFFFFF, 0.28f)}
        );
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setStroke(dp(context, 5), 0x55FFFFFF);
        return drawable;
    }

    static void stylePrimaryButton(Button button, Context context, int colorId) {
        button.setTextColor(0xFFFFFFFF);
        button.setBackground(pill(context, colorId));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
    }

    static void styleSoftButton(Button button, Context context, int colorId, boolean danger) {
        button.setTextColor(color(context, danger ? colorId : R.color.text));
        button.setBackground(outline(context, colorId, true));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
    }

    static void noDefaultButtonShadow(View view) {
        view.setStateListAnimator(null);
    }

    static int topInset(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBar = resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : dp(context, 24);
        return statusBar + dp(context, 18);
    }

    private static int softFillColor(int color) {
        return mix(color, 0xFFFFFFFF, 0.84f);
    }

    private static int mix(int color, int target, float targetWeight) {
        int a = 0xFF;
        int r = Math.round(((color >> 16) & 0xFF) * (1 - targetWeight) + ((target >> 16) & 0xFF) * targetWeight);
        int g = Math.round(((color >> 8) & 0xFF) * (1 - targetWeight) + ((target >> 8) & 0xFF) * targetWeight);
        int b = Math.round((color & 0xFF) * (1 - targetWeight) + (target & 0xFF) * targetWeight);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int color(Context context, int colorId) {
        return context.getResources().getColor(colorId, context.getTheme());
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
