package max.music_cyclon.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import max.music_cyclon.SynchronizeActivity;
import max.music_cyclon.SynchronizeConfig;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
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
            // todo the latest changes, if the app is running are not available at this point

            List<SynchronizeConfig> loadedConfigs = Collections.emptyList();
            try {
                FileInputStream in = context.openFileInput(SynchronizeActivity.DEFAULT_CONFIG_PATH);
                loadedConfigs = SynchronizeConfig.load(in);
                in.close();
            } catch (IOException | JSONException e) {
                Log.e("CONFIG", "Failed loading the config", e);
            }

            List<SynchronizeConfig> configs = new ArrayList<>();

            for (SynchronizeConfig config : loadedConfigs) {
                if (!config.isStartCharging(context.getResources())) {
                    continue;
                }

                long lastUpdated = config.getLastUpdated();

                if (lastUpdated < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(config.getDownloadInterval(context.getResources()))) {
                    configs.add(config);
                    config.updateLastUpdated(); // fixme the result of LibraryService does not affect this (for example: Remote not available)
                }
            }

            if (configs.isEmpty()) {
                return;
            }

            Intent serviceIntend = new Intent(context, LibraryService.class);
            serviceIntend.putExtra(
                    LibraryService.ARGUMENT_CONFIGS,
                    configs.toArray(new SynchronizeConfig[configs.size()])
            );

            context.startService(serviceIntend);

            try {
                FileOutputStream fos = context.openFileOutput(SynchronizeActivity.DEFAULT_CONFIG_PATH, Context.MODE_PRIVATE);
                SynchronizeConfig.save(loadedConfigs, fos);
                fos.close();
            } catch (IOException | JSONException e) {
                Log.e("CONFIG", "Failed saving the config", e);
            }
        }
    }
}