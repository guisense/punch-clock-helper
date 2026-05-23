package com.daniel.punchclock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;

final class ReminderScheduler {
    static final String CHANNEL_ID = "safe_clock_out";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_BODY = "body";

    private ReminderScheduler() {
    }

    static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "安全下班提醒",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("提醒安全下班時間，避免提前打卡。");

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        NotificationChannel countdown = new NotificationChannel(
                CountdownNotifier.CHANNEL_ID,
                "下班倒計時",
                NotificationManager.IMPORTANCE_LOW
        );
        countdown.setDescription("常駐顯示今日安全下班倒計時。");
        manager.createNotificationChannel(countdown);
    }

    static void scheduleSafeClockOut(Context context, WorkRecord record, WorkSettings settings) {
        cancelToday(context);
        LocalDateTime safeClockOut = record.safeClockOut(settings);
        if (safeClockOut == null) {
            return;
        }

        LocalDateTime fiveMinutesBefore = safeClockOut.minusMinutes(5);
        if (fiveMinutesBefore.isAfter(LocalDateTime.now())) {
            schedule(
                    context,
                    1001,
                    "快到安全下班時間",
                    "再等 5 分鐘左右就可以安全下班了。",
                    fiveMinutesBefore
            );
        }

        if (safeClockOut.isAfter(LocalDateTime.now())) {
            schedule(
                    context,
                    1002,
                    "可以安全下班了",
                    "已滿 8 小時並加上安全緩衝，刷臉後再按下班。",
                    safeClockOut
            );
        }
    }

    static void cancelToday(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent(context, 1001, "", ""));
        alarmManager.cancel(pendingIntent(context, 1002, "", ""));
    }

    private static void schedule(Context context, int requestCode, String title, String body, LocalDateTime time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        PendingIntent intent = pendingIntent(context, requestCode, title, body);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent);
            return;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent);
    }

    private static PendingIntent pendingIntent(Context context, int requestCode, String title, String body) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_BODY, body);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
