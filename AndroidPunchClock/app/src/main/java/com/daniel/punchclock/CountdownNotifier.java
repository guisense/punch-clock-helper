package com.daniel.punchclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

final class CountdownNotifier {
    static final String CHANNEL_ID = "clock_out_countdown";
    static final String ACTION_UPDATE = "com.daniel.punchclock.UPDATE_COUNTDOWN";
    private static final int NOTIFICATION_ID = 3301;
    private static final int REQUEST_UPDATE = 3302;

    private CountdownNotifier() {
    }

    static void update(Context context) {
        ReminderScheduler.ensureNotificationChannel(context);
        AttendanceStore store = new AttendanceStore(context);
        WorkSettings settings = new WorkSettings(context);
        WorkRecord record = store.todayRecord();

        if (record.clockIn == null || record.clockOut != null) {
            cancel(context);
            CountdownWidgetProvider.updateAll(context);
            return;
        }

        LocalDateTime safe = record.safeClockOut(settings);
        if (safe == null) {
            cancel(context);
            CountdownWidgetProvider.updateAll(context);
            return;
        }

        String title = "安全下班 " + Formatters.time(safe);
        String body = countdownText(safe);
        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentIntent(openAppIntent(context));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (ReminderScheduler.hasNotificationPermission(context)) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }

        scheduleNextUpdate(context);
        CountdownWidgetProvider.updateAll(context);
    }

    static void cancel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(updateIntent(context));
        CountdownWidgetProvider.updateAll(context);
    }

    static String countdownText(LocalDateTime safe) {
        long minutes = Duration.between(LocalDateTime.now(), safe).toMinutes();
        if (minutes <= 0) {
            return "已經可以安全下班";
        }
        return "距離安全下班還有 " + Formatters.remainingMinutes(Math.max(1, minutes));
    }

    private static void scheduleNextUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long next = LocalDateTime.now()
                .plusMinutes(1)
                .withSecond(2)
                .withNano(0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        PendingIntent intent = updateIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent);
    }

    private static PendingIntent updateIntent(Context context) {
        Intent intent = new Intent(context, CountdownReceiver.class);
        intent.setAction(ACTION_UPDATE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_UPDATE, intent, flags);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}
