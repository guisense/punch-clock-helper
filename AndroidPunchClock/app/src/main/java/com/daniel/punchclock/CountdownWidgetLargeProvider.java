package com.daniel.punchclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

public final class CountdownWidgetLargeProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            WidgetRenderer.update(context, manager, id, R.layout.widget_countdown_large, 3);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountdownWidgetLargeProvider.class));
        for (int id : ids) {
            WidgetRenderer.update(context, manager, id, R.layout.widget_countdown_large, 3);
        }
    }
}
