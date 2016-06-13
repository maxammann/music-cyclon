package max.music_cyclon.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.Log;

import com.maxmpz.poweramp.player.PowerampAPI;


import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import max.music_cyclon.SynchronizeConfig;
import max.music_cyclon.service.db.FileTracker;

public class LibraryService extends IntentService {


    public static final FilenameFilter NOMEDIA_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File file, String s) {
            return !s.equals(".nomedia");
        }
    };

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;


    public static final int MSG_CANCEL = 3;
    public static final int MSG_STARTED = 4;
    public static final int MSG_FINISHED = 5;
    public static final Random RANDOM = new Random();

    /**
     * Keeps track of all current registered clients.
     */
    private ArrayList<Messenger> mClients = new ArrayList<>();

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    public LibraryService() {
        super("max.music_cyclon.service.LibraryService");
    }

    public List<Item> fetchRandom(String address, SynchronizeConfig config, Resources resources) throws IOException {
        StringBuilder get;

        if (config.isAlbum(resources)) {
            get = new StringBuilder("/album");
        } else {
            get = new StringBuilder("/item");
        }

        String query = config.getQuery(resources);
        if (!query.isEmpty()) {
            get.append("/query/").append(query);
        }

        get.append("?expand");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(address + get);
        CloseableHttpResponse response = httpclient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.getStatusLine().getStatusCode());
            return Collections.emptyList();
        }


        InputStream stream = response.getEntity().getContent();
        ArrayList<Item> items = parseJson(stream, config.getSize(resources));
        stream.close();

        return items;
    }

    private ArrayList<Item> parseJson(InputStream stream, int size) throws IOException {
        JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(stream, "UTF-8")));

        ArrayList<Item> items = new ArrayList<>();
        ArrayList<ArrayList<Item>> albums = new ArrayList<>();

        reader.beginObject();
        boolean isAlbums = reader.nextName().equals("albums");
        reader.beginArray();
        while (reader.hasNext()) {
            if (isAlbums) {
                albums.add(parseAlbum(reader));
            } else {
                items.add(parseItem(reader));
            }
        }
        reader.endArray();
        reader.endObject();

        items = selectRandom(items, size);

        ArrayList<ArrayList<Item>> randomAlbums = selectRandom(albums, size);

        for (ArrayList<Item> album : randomAlbums) {
            items.addAll(album);
        }

        return items;
    }

    public <T> ArrayList<T> selectRandom(ArrayList<T> list, int n) {
        ArrayList<T> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(list.get(RANDOM.nextInt(list.size() - 1)));
        }

        return out;
    }

    private ArrayList<Item> parseAlbum(JsonReader reader) throws IOException {
        reader.beginObject();

        ArrayList<Item> items = new ArrayList<>();

        while (reader.hasNext()) {
            String tag = reader.nextName();
            if (tag.equals("items")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    items.add(parseItem(reader));
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();

        return items;
    }

    private Item parseItem(JsonReader reader) throws IOException {
        reader.beginObject();
        Item item = new Item();

        while (reader.hasNext()) {
            String tag = reader.nextName();
            switch (tag) {
                case "id":
                    item.setId(reader.nextInt());
                    break;
                case "title":
                    item.setName(reader.nextString());
                    break;
                case "album":
                    item.setAlbum(reader.nextString());
                    break;
                case "artist":
                    item.setArtist(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return item;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        broadcast(Message.obtain(null, MSG_STARTED));

        Parcelable[] configs = intent.getParcelableArrayExtra("configs");

        SharedPreferences globalSettings = PreferenceManager.getDefaultSharedPreferences(this);
        ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(globalSettings.getString("threads", "2")));
        String address = globalSettings.getString("address", "127.0.0.1");
        File root = new File(Environment.getExternalStorageDirectory(), "library");
        ProgressUpdater updater = new ProgressUpdater(this);

        if (root.exists() && !root.isDirectory()) {
            updater.showMessage("Library is no dictionary! Fix manually");
            return;
        }

        root.mkdirs();

        FileTracker tracker = new FileTracker(getApplicationContext());

        try {
            updater.showMessage("Cleaning library");
            tracker.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root.exists() && root.list(NOMEDIA_FILTER).length != 0) {
            updater.showMessage("Library not empty! Clean in manually");
            return;
        }

        for (Parcelable parcelable : configs) {
            SynchronizeConfig config = (SynchronizeConfig) parcelable;
            List<Item> items;
            try {
                updater.showMessage("Fetching music information for %s", config.getName());
                items = fetchRandom(address, config, getResources());
            } catch (IOException e) {
                Log.wtf("WTF", e);
                updater.showMessage("Remote not available");
                return;
            }

            updater.showMessage("Mixing new music for %s!", config.getName());
            updater.setMaximumProgress(items.size());

            CountDownLatch itemsLeftLatch = new CountDownLatch(items.size());

            for (Item item : items) {
                try {
                    executor.submit(new DownloadTask(new URL(address + "/item/" + item.getId() + "/file").toURI(), item.getArtist() + "/" + item.getAlbum() + "/" + item.getName() + ".mp3", tracker, updater, itemsLeftLatch));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            try {
                itemsLeftLatch.await();
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        updater.showMessage("Musik aktualisiert");

        // Update last_updated info
        SharedPreferences preferences = getSharedPreferences("info", MODE_PRIVATE);
        preferences.edit().putLong("last_updated", System.currentTimeMillis()).apply();

        // Poweramp support
        Intent poweramp = new Intent(PowerampAPI.Scanner.ACTION_SCAN_DIRS);
        poweramp.setPackage(PowerampAPI.PACKAGE_NAME);
        poweramp.putExtra(PowerampAPI.Scanner.EXTRA_FULL_RESCAN, true);
        startService(poweramp);

        broadcast(Message.obtain(null, MSG_FINISHED));
    }

    /**
     * Handler of incoming messages from clients.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
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
