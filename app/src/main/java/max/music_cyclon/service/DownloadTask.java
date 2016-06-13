package max.music_cyclon.service;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.zip.Adler32;

import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import max.music_cyclon.service.db.FileTracker;

public class DownloadTask implements Runnable {

    private final URI uri;
    private final String itemPath;

    private final FileTracker tracker;
    private final ProgressUpdater progressUpdater;
    private CountDownLatch itemsLeftLatch;

    private static final CloseableHttpClient httpclient = HttpClients.createDefault();

    public DownloadTask(URI uri, String itemPath,
                        FileTracker tracker, ProgressUpdater progressUpdater,
                        CountDownLatch itemsLeftLatch) {
        this.uri = uri;
        this.itemPath = itemPath;

        this.tracker = tracker;
        this.progressUpdater = progressUpdater;
        this.itemsLeftLatch = itemsLeftLatch;
    }

    private InputStream prepareConnection() throws IOException {
        HttpGet httpGet = new HttpGet(uri);

        CloseableHttpResponse response = httpclient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.getStatusLine().getStatusCode());
            return null;
        }

        return response.getEntity().getContent();
    }

    @Override
    public void run() {
        File root = new File(Environment.getExternalStorageDirectory(), "library");

        try {
            File target = new File(root, itemPath);
            Adler32 checksum = new Adler32();

            InputStream input = prepareConnection();

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

            tracker.track(target, checksum.getValue());
        } catch (IOException e) {
            Log.wtf("WTF", e);
        }

        progressUpdater.increment();
        itemsLeftLatch.countDown();
    }
}
