package max.music_cyclon;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.Adler32;

public class FileTracker {

    private SharedPreferences.Editor editor;
    private SharedPreferences preferences;

    @SuppressLint("CommitPrefEdits")
    public FileTracker(SharedPreferences preferences) {
        this.preferences = preferences;
        this.editor = preferences.edit();
    }

    public void track(File file, long checksum) {
        editor.putLong(file.getAbsolutePath(), checksum);
    }

    public void delete() throws IOException {
        Map<String, ?> preferences = this.preferences.getAll();

        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            if (!(entry.getValue() instanceof Long)) {
                continue;
            }

            File file = new File(entry.getKey());

            if (((Long) entry.getValue()) != checksum(file)) {
                continue;
            }

            removeFile(file);
        }
    }

    private long checksum(File file) throws IOException {
        if (!file.exists()) {
            return 0;
        }

        Adler32 checksum = new Adler32();
        byte[] buffer = new byte[4 * 1024];
        int n;

        FileInputStream input = FileUtils.openInputStream(file);

        while (-1 != (n = input.read(buffer))) {
            checksum.update(buffer, 0, n);
        }

        input.close();

        return checksum.getValue();
    }

    public void removeFile(File path) throws IOException {
        if (path == null) return;

        if (path.isFile()) {
            FileUtils.deleteQuietly(path);
        } else if (path.isDirectory()) {
            if (!path.delete()) {
                return;
            }
        }

        removeFile(path.getParentFile());
    }

    public void commit() {
        editor.apply();
    }
}
