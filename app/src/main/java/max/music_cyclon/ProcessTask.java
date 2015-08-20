package max.music_cyclon;

import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;

public class ProcessTask implements Runnable {

    private String address;
    private final CountDownLatch latch;

    private final String item;
    private final FileTracker tracker;

    private final AtomicInteger current;
    private final int maximum;
    private final NotificationCompat.Builder builder;
    private final NotificationManagerCompat notificationManager;

    public ProcessTask(String address, CountDownLatch latch, String item, FileTracker tracker,
                       AtomicInteger current, int maximum, NotificationCompat.Builder builder, NotificationManagerCompat notificationCompat) {
        this.address = address;
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

            InputStream input = prepareConnection(address, item);

            if (input != null) {

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
            }

//          todo else error

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

    public InputStream prepareConnection(String address, String item) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(address + "/get");

        httpPost.setEntity(new ByteArrayEntity(item.getBytes("UTF-8")));

        CloseableHttpResponse response = httpclient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.getStatusLine().getStatusCode());
            return null;
        }

        return response.getEntity().getContent();
    }
}
