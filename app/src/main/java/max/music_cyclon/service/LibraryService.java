package max.music_cyclon.service;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.maxmpz.poweramp.player.PowerampAPI;


import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import max.music_cyclon.R;
import max.music_cyclon.SynchronizeConfig;
import max.music_cyclon.tracker.FileTracker;

public class LibraryService extends IntentService {

    /**
     * Command to the serviceReference to register a client, receiving callbacks
     * from the serviceReference.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the serviceReference to unregister a client, ot stop receiving callbacks
     * from the serviceReference.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;


    public static final int MSG_CANCEL = 3;
    public static final int MSG_STARTED = 4;
    public static final int MSG_FINISHED = 5;

    /**
     * Keeps track of all current registered clients.
     */
    private ArrayList<Messenger> mClients = new ArrayList<>();

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(
            new IncomingHandler(new WeakReference<>(this))
    );

    public LibraryService() {
        super(LibraryService.class.getName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Parcelable[] configs = intent.getParcelableArrayExtra("configs");

        ProgressUpdater updater = new ProgressUpdater(this);

        broadcast(Message.obtain(null, MSG_STARTED));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            updater.showMessage("No permission to write!", false);
            finished();
            return;
        }

        SharedPreferences globalSettings = PreferenceManager.getDefaultSharedPreferences(this);
        int threads = Integer.parseInt(globalSettings.getString("threads", Integer.toString(getResources().getInteger(R.integer.threads))));
        String address = globalSettings.getString("address", getResources().getString(R.string.address));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        File root = new File(Environment.getExternalStorageDirectory(), "library");

        BeetsFetcher fetcher = new BeetsFetcher(address, getResources());

        if (root.exists() && !root.isDirectory()) {
            updater.showMessage("Library is no dictionary! Fix manually", false);
            finished();
            return;
        }

        root.mkdirs();

        FileTracker tracker = new FileTracker(getApplicationContext());

        try {
            updater.showMessage("Cleaning library", true);
            tracker.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root.exists() && root.list().length != 0) {
            updater.showMessage("Library not empty! Clean in manually", false);
            finished();
            return;
        }

        ArrayList<DownloadTask> tasks = new ArrayList<>();

        for (Parcelable parcelable : configs) {
            SynchronizeConfig config = (SynchronizeConfig) parcelable;
            List<Item> items;
            try {
                updater.showMessage("Fetching music information for %s", true, config.getName());
                items = fetcher.fetch(config);
            } catch (IOException e) {
                Log.wtf("WTF", e);
                updater.showMessage("Remote not available", false);
                finished();
                return;
            }

            updater.showMessage("Mixing new music for %s!", true, config.getName());
            updater.setMaximumProgress(items.size());

            for (Item item : items) {
                try {
                    URI uri = new URI(address + "/item/" + item.getId() + "/file");
                    tasks.add(new DownloadTask(config, uri, item.getPath(), tracker, updater));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }

        CountDownLatch itemsLeftLatch = new CountDownLatch(tasks.size());

        for (DownloadTask task : tasks) {
            task.setItemsLeftLatch(itemsLeftLatch);
            executor.submit(task);
        }

        try {
            itemsLeftLatch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        updater.showMessage("Musik aktualisiert", false);

        // Update last_updated info
        SharedPreferences preferences = getSharedPreferences("info", MODE_PRIVATE);
        preferences.edit().putLong("last_updated", System.currentTimeMillis()).apply();

        // Poweramp support
        Intent poweramp = new Intent(PowerampAPI.Scanner.ACTION_SCAN_DIRS);
        poweramp.setPackage(PowerampAPI.PACKAGE_NAME);
        poweramp.putExtra(PowerampAPI.Scanner.EXTRA_FULL_RESCAN, true);
        startService(poweramp);

        finished();
    }

    public void finished() {
        broadcast(Message.obtain(null, MSG_FINISHED));
    }

    /**
     * Handler of incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {

        private final WeakReference<LibraryService> serviceReference;

        private IncomingHandler(WeakReference<LibraryService> serviceReference) {
            this.serviceReference = serviceReference;
        }

        @Override
        public void handleMessage(Message msg) {
            LibraryService service = serviceReference.get();

            if (service == null) {
                return;
            }

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    service.mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    service.mClients.remove(msg.replyTo);
                    break;
                case MSG_CANCEL:
                    //todo
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void broadcast(Message msg) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
