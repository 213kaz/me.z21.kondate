package me.z21.kondate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");

        String action = intent.getAction();
        if (Objects.requireNonNull(action).equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG,"システム起動のお知らせを受信しました。");
            Intent intentActivity = new Intent(context, FullscreenActivity.class);
            intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentActivity);
        }

    }
}