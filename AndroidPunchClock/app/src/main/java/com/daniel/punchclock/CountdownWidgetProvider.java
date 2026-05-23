package com.daniel.punchclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.time.LocalDateTime;

public final class CountdownWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, CountdownWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_countdown);
        AttendanceStore store = new AttendanceStore(context);
        WorkSettings settings = new WorkSettings(context);
        WorkRecord record = store.todayRecord();

        if (record.clockIn == null) {
            views.setTextViewText(R.id.widget_title, "今日尚未上班");
            views.setTextViewText(R.id.widget_time, "--:--");
            views.setTextViewText(R.id.widget_subtitle, "點擊打開打卡助手");
        } else if (record.clockOut != null) {
            views.setTextViewText(R.id.widget_title, "今日已完成");
            views.setTextViewText(R.id.widget_time, Formatters.time(record.clockOut));
            views.setTextViewText(R.id.widget_subtitle, "實際 " + Formatters.workedTime(record.workedMinutes(settings)));
        } else {
            LocalDateTime safe = record.safeClockOut(settings);
            views.setTextViewText(R.id.widget_title, "安全下班");
            views.setTextViewText(R.id.widget_time, Formatters.time(safe));
            views.setTextViewText(R.id.widget_subtitle, CountdownNotifier.countdownText(safe));
        }

        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context));
        manager.updateAppWidget(appWidgetId, views);
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
