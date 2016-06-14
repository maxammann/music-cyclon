package max.music_cyclon.tracker;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.Adler32;

import max.music_cyclon.SynchronizeConfig;

public class FileTracker {

    private final LibraryDBOpenHelper helper;

    public FileTracker(Context context) {
        helper = new LibraryDBOpenHelper(context);
    }

    public void track(SynchronizeConfig config, File file, long checksum) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("path", file.getAbsolutePath());
        values.put("checksum", checksum);
        values.put("config", config.getID());

        db.insert("library", null, values);
        db.close();
    }

    public void delete() throws IOException {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query("library", null, null, null, null, null, null);
        int pathIndex = cursor.getColumnIndex("path");
        int checksumIndex = cursor.getColumnIndex("checksum");

        while (cursor.moveToNext()) {
            long checksum = cursor.getLong(checksumIndex);
            String path = cursor.getString(pathIndex);


            File file = new File(path);

            if (checksum != checksum(file)) {
                continue;
            }

            removeFile(file);
        }

        cursor.close();
        db.close();
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
}
