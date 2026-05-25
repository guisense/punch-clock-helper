package com.daniel.punchclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

public final class CountdownWidgetSmallProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            WidgetRenderer.update(context, manager, id, R.layout.widget_countdown_small, 1);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountdownWidgetSmallProvider.class));
        for (int id : ids) {
            WidgetRenderer.update(context, manager, id, R.layout.widget_countdown_small, 1);
        }
    }
}
