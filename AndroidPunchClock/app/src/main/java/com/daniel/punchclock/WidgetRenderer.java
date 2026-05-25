package com.daniel.punchclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.time.LocalDateTime;

final class WidgetRenderer {
    private WidgetRenderer() {
    }

    static void update(Context context, AppWidgetManager manager, int appWidgetId, int layoutId, int mode) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        AttendanceStore store = new AttendanceStore(context);
        WorkSettings settings = new WorkSettings(context);
        WorkRecord record = store.todayRecord();

        if (mode == 1) {
            renderSmall(views, record, settings);
        } else if (mode == 3) {
            renderLarge(views, record, settings);
        } else {
            renderMedium(views, record, settings);
        }
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
    }

    private static void renderSmall(RemoteViews views, WorkRecord record, WorkSettings settings) {
        if (record.clockIn == null) {
            views.setTextViewText(R.id.widget_title, "安全下班");
            views.setTextViewText(R.id.widget_time, "--:--");
            views.setTextViewText(R.id.widget_subtitle, "尚未上班");
            return;
        }

        if (record.clockOut != null) {
            views.setTextViewText(R.id.widget_title, "今日完成");
            views.setTextViewText(R.id.widget_time, Formatters.time(record.clockOut));
            views.setTextViewText(R.id.widget_subtitle, "已下班");
            return;
        }

        LocalDateTime safe = record.safeClockOut(settings);
        views.setTextViewText(R.id.widget_title, "安全下班");
        views.setTextViewText(R.id.widget_time, Formatters.time(safe));
        views.setTextViewText(R.id.widget_subtitle, CountdownNotifier.countdownText(safe));
    }

    private static void renderMedium(RemoteViews views, WorkRecord record, WorkSettings settings) {
        if (record.clockIn == null) {
            views.setTextViewText(R.id.widget_title, "打卡助手");
            views.setTextViewText(R.id.widget_time, "--:--");
            views.setTextViewText(R.id.widget_subtitle, "尚未上班");
            views.setTextViewText(R.id.widget_extra, "點擊打開應用");
            return;
        }

        if (record.clockOut != null) {
            views.setTextViewText(R.id.widget_title, "上班 " + Formatters.time(record.clockIn));
            views.setTextViewText(R.id.widget_time, Formatters.time(record.clockOut));
            views.setTextViewText(R.id.widget_subtitle, "今日已完成");
            views.setTextViewText(R.id.widget_extra, "實際 " + Formatters.workedTime(record.workedMinutes(settings)));
            return;
        }

        LocalDateTime safe = record.safeClockOut(settings);
        views.setTextViewText(R.id.widget_title, "上班 " + Formatters.time(record.clockIn));
        views.setTextViewText(R.id.widget_time, Formatters.time(safe));
        views.setTextViewText(R.id.widget_subtitle, "安全下班");
        views.setTextViewText(R.id.widget_extra, CountdownNotifier.countdownText(safe));
    }

    private static void renderLarge(RemoteViews views, WorkRecord record, WorkSettings settings) {
        if (record.clockIn == null) {
            views.setTextViewText(R.id.widget_title, "上班時間");
            views.setTextViewText(R.id.widget_time, "--:--");
            views.setTextViewText(R.id.widget_subtitle, "尚未打卡上班");
            views.setTextViewText(R.id.widget_extra, "工時目標 " + Formatters.workedTime(settings.requiredMinutes()));
            views.setTextViewText(R.id.widget_detail_one, "安全緩衝 " + settings.safetyBufferText());
            views.setTextViewText(R.id.widget_detail_two, settings.deductLunch() ? "午休扣除 " + Formatters.remainingMinutes(settings.lunchMinutes()) : "不扣除午休");
            return;
        }

        if (record.clockOut != null) {
            views.setTextViewText(R.id.widget_title, "上班時間");
            views.setTextViewText(R.id.widget_time, Formatters.time(record.clockIn));
            views.setTextViewText(R.id.widget_subtitle, "今日已打卡上班");
            views.setTextViewText(R.id.widget_extra, "下班時間 " + Formatters.time(record.clockOut));
            views.setTextViewText(R.id.widget_detail_one, "實際工時 " + Formatters.workedTime(record.workedMinutes(settings)));
            views.setTextViewText(R.id.widget_detail_two, statusText(record, settings));
            return;
        }

        LocalDateTime safe = record.safeClockOut(settings);
        views.setTextViewText(R.id.widget_title, "上班時間");
        views.setTextViewText(R.id.widget_time, Formatters.time(record.clockIn));
        views.setTextViewText(R.id.widget_subtitle, "今日已打卡上班");
        views.setTextViewText(R.id.widget_extra, "安全下班 " + Formatters.time(safe));
        views.setTextViewText(R.id.widget_detail_one, CountdownNotifier.countdownText(safe));
        views.setTextViewText(R.id.widget_detail_two, "安全緩衝 " + settings.safetyBufferText());
    }

    private static String statusText(WorkRecord record, WorkSettings settings) {
        long overtime = record.overtimeMinutes(settings);
        if (overtime <= 0) {
            return "狀態 正常";
        }
        return "加班 " + Formatters.remainingMinutes(overtime);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 10, intent, flags);
    }
}
