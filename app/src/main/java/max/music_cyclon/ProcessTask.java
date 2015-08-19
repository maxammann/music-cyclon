package max.music_cyclon;

import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;

public class ProcessTask implements Runnable {

    private final CountDownLatch latch;

    private final String item;
    private final FileTracker tracker;

    private final AtomicInteger current;
    private final int maximum;
    private final NotificationCompat.Builder builder;
    private final NotificationManagerCompat notificationManager;

    public ProcessTask(CountDownLatch latch, String item, FileTracker tracker,
                       AtomicInteger current, int maximum,NotificationCompat.Builder builder, NotificationManagerCompat notificationCompat) {
        this.latch = latch;
        this.item = item;

        this.tracker = tracker;
        this.current = current;
        this.maximum = maximum;
        this.builder = builder;
        this.notificationManager = notificationCompat;
    }

    @Override
    public void run() {
        File root = new File(Environment.getExternalStorageDirectory(), "library");

        try {
            File target = new File(root, item);
            Adler32 checksum = new Adler32();

//          builder.setContentText(item);
//          notificationManager.notify(NOTIFICATION_ID, builder.build());


            HttpURLConnection connection = prepareConnection(item);

            if (connection == null) {
                return;
            }


            InputStream input = connection.getInputStream();
            FileOutputStream output = FileUtils.openOutputStream(target);

            byte[] buffer = new byte[4 * 1024];
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                checksum.update(buffer, 0, n);
            }

            output.flush();
            output.close();
            input.close();

            tracker.track(target, checksum.getValue());

            latch.countDown();

            int current = this.current.incrementAndGet();
            builder.setProgress(maximum, current, false);
            builder.setContentText(current + "/" + maximum);
            notificationManager.notify(LibraryService.NOTIFICATION_ID, builder.build());
        } catch (IOException e) {
            Log.wtf("WTF", e);
        }
    }

    public HttpURLConnection prepareConnection(String item) throws IOException {

        URL url = new URL("http", "max-arch", 5000, "/get");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        PrintWriter output = new PrintWriter(connection.getOutputStream());
        output.write(item);
        output.flush();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.e("ERROR", "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
            return null;
        }

        return connection;
    }
}
