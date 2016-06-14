package max.music_cyclon.tracker;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import max.music_cyclon.service.ProgressUpdater;


public class ForceClearReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        File root = new File(Environment.getExternalStorageDirectory(), "library");
        try {
            FileUtils.deleteDirectory(root);
        } catch (IOException e) {
            Log.e("LIBRARY", "Failed to delete library", e);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(ProgressUpdater.NOTIFICATION_ID);
    }
}