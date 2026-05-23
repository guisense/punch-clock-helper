package com.daniel.punchclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class CountdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        CountdownNotifier.update(context);
    }
}
