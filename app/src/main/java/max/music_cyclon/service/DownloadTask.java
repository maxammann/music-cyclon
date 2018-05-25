package max.music_cyclon.service;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.zip.Adler32;

import max.music_cyclon.SynchronizeConfig;
import max.music_cyclon.tracker.FileTracker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask implements Runnable {

    private final SynchronizeConfig config;
    private final String url;
    private final String itemPath;
    private final String libraryPath;
    private final String username;
    private final String password;
    private final FileTracker tracker;
    private final ProgressUpdater progressUpdater;
    private CountDownLatch itemsLeftLatch;
    public static final OkHttpClient CLIENT = new OkHttpClient();


    public DownloadTask(SynchronizeConfig config, String url,
                        String libraryPath, String itemPath,
                        FileTracker tracker, ProgressUpdater progressUpdater,
                        String username, String password
    ) {
        this.config = config;
        this.url = url;
        this.itemPath = itemPath;
        this.libraryPath = libraryPath;

        this.username = username;
        this.password = password;

        this.tracker = tracker;
        this.progressUpdater = progressUpdater;
    }

    private InputStream prepareConnection() throws IOException {
        String auth = okhttp3.Credentials.basic(username != null ? username : "",
                password != null ? password : "");
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .build();

        Response response = CLIENT.newCall(request).execute();

        if (response.code() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.message());
            return null;
        }

        return response.body().byteStream();
    }

    public void setItemsLeftLatch(CountDownLatch itemsLeftLatch) {
        this.itemsLeftLatch = itemsLeftLatch;
    }

    @Override
    public void run() {
        File root = new File(Environment.getExternalStorageDirectory(),
                libraryPath);
        if (itemPath != null) {
            try {
                File target = new File(root, itemPath);
                if (! target.exists()) {
                    Adler32 checksum = new Adler32();

                    InputStream input = prepareConnection();

                    if (input != null) {
                        Log.d("DOWNLOAD", "Writing file: " + target);
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

                    tracker.track(config, target, checksum.getValue());
                }
            } catch (IOException e) {
                Log.e("DOWNLOAD", "Failed to download", e);
            }
            Log.i("DOWNLOAD", "Success");
        } else {
            Log.e("DOWNLOAD", "Missing download path, FAILED!");
        }
        progressUpdater.increment();
        itemsLeftLatch.countDown();
    }
}
