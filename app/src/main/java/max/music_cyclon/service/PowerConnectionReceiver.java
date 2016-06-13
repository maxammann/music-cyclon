package max.music_cyclon.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean start = settings.getBoolean("start_charging", false);

        if (!start) {
            return;
        }


        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus == null) {
            return;
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        if (acCharge && isCharging) {

            SharedPreferences preferences = context.getSharedPreferences("info", Context.MODE_PRIVATE);
            long lastUpdated = preferences.getLong("last_updated", 0);

            if (lastUpdated < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Integer.parseInt(settings.getString("min_download_interval", "7")))) {
                Intent serviceIntend = new Intent(context, LibraryService.class);

                context.startService(serviceIntend);
            }
        }
    }
}