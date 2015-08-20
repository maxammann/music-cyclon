package max.music_cyclon;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LibraryService extends IntentService {

    private final String host;
    private final int port;

    public static int NOTIFICATION_ID = new Random().nextInt();

    public LibraryService() {
        this("max-arch", 5000);
    }

    public LibraryService(String host, int port) {
        super("max.music_cyclon.LibraryService");
        this.host = host;
        this.port = port;
    }

    public List<String> fetchRandom(String address, int amount) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(address + "/random/" + amount);


        CloseableHttpResponse response = httpclient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.getStatusLine().getStatusCode());
            return Collections.emptyList();
        }

        JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8")));

        ArrayList<String> items = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            items.add(reader.nextString());
        }
        reader.endArray();

        return items;
    }

    private NotificationCompat.Builder notificationBuilder() {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher);
    }

    private NotificationCompat.Builder progressNotificationBuilder() {
        return notificationBuilder().setUsesChronometer(true)
                .setOngoing(true)
                .setProgress(0, 0, true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(settings.getString("threads", "2")));

        String address = settings.getString("address", "127.0.0.1");


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = progressNotificationBuilder().setContentTitle("Aktualisiere Musik");
        File root = new File(Environment.getExternalStorageDirectory(), "library");

        if (root.exists() && !root.isDirectory()) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder().setContentTitle("Library is no dictionary! Fix manually").build());
            return;
        }

        AtomicInteger current = new AtomicInteger();

        FileTracker tracker = new FileTracker(getSharedPreferences("library", MODE_PRIVATE));


        List<String> items;
        try {
            builder.setContentTitle("Fetching music information");
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            items = fetchRandom(address, Integer.parseInt(settings.getString("download", "10")));

            builder.setContentTitle("Cleaning library");
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            tracker.delete();
        } catch (IOException e) {
            Log.wtf("WTF", e);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder().setContentTitle("Remote not available").build());
            return;
        }


        if (root.exists() && root.list().length != 0) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder().setContentTitle("Library not empty! Clean in manually").build());
            return;
        }

        builder.setContentTitle("Mixing new music!");
        builder.setProgress(items.size(), 0, false);
        notificationManager.notify(NOTIFICATION_ID, builder.build());


        CountDownLatch latch = new CountDownLatch(items.size());

        for (int i = 0, size = items.size(); i < size; i++) {
            executor.submit(new ProcessTask(address, latch, items.get(i), tracker,
                    current, items.size(), builder, notificationManager));
        }

        try {
            latch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tracker.commit();

        notificationManager.cancel(NOTIFICATION_ID);

        SharedPreferences preferences = getSharedPreferences("info", MODE_PRIVATE);
        preferences.edit().putLong("last_updated", System.currentTimeMillis()).apply();


        notificationManager.notify(NOTIFICATION_ID, notificationBuilder().setContentTitle("Musik aktualisiert").build());
    }
}
