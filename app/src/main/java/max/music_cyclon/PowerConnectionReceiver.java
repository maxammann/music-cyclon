package max.music_cyclon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);


        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
         boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        if (acCharge && isCharging) {
//            Intent serviceIntend = new Intent(context, LibraryService.class);
//
//            context.startService(serviceIntend);
        }
    }
}