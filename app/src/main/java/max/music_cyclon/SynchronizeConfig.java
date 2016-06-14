package max.music_cyclon;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * A synchronize config which holds the information which tracks/albums should be queried
 */
public class SynchronizeConfig implements Parcelable {

    private static final Random RANDOM = new Random();

    private static final String ID_KEY = "id";
    private static final String SIZE_KEY = "size";
    private static final String RANDOM_KEY = "random";
    private static final String ALBUM_KEY = "use_albums";
    private static final String QUERY_KEY = "query";
    private static final String START_CHARGING_KEY = "start_charging";
    private static final String DOWNLOAD_INTERVAL_KEY = "download_interval";

    /**
     * The name of this config
     */
    private String name;

    /**
     * The data structure
     */
    private JSONObject json;

    public SynchronizeConfig(String name, JSONObject json) {
        this.name = name;
        this.json = json;
    }

    public SynchronizeConfig(String name, long id) {
        this(name, new JSONObject());
        try {
            json.put(ID_KEY, id);
        } catch (JSONException e) {
            throw new RuntimeException("Failed setting id!");
        }
    }

    public SynchronizeConfig(String name) {
        this(name, randomID(name));
    }

    public long getID() {
        try {
            return json.getLong(ID_KEY);
        } catch (JSONException e) {
            throw new RuntimeException("Config has no id!");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastUpdated() {
        return json.optLong("last_update", 0);
    }

    public void updateLastUpdated() {
        try {
            json.put("last_update", System.currentTimeMillis());
        } catch (JSONException ignored) {
            throw new RuntimeException("Failed to set last_update!");
        }
    }

    public int getSize(Resources resources) {
        return json.optInt(SIZE_KEY, resources.getInteger(R.integer.size));
    }

    public boolean isRandom(Resources resources) {
        return json.optBoolean(RANDOM_KEY, resources.getBoolean(R.bool.random));
    }

    public boolean isAlbum(Resources resources) {
        return json.optBoolean(ALBUM_KEY, resources.getBoolean(R.bool.use_albums));
    }

    public String getQuery(Resources resources) {
        return json.optString(QUERY_KEY, resources.getString(R.string.query));
    }

    public boolean isStartCharging(Resources resources) {
        return json.optBoolean(START_CHARGING_KEY, resources.getBoolean(R.bool.start_charging));
    }

    public int getDownloadInterval(Resources resources) {
        return json.optInt(DOWNLOAD_INTERVAL_KEY,
                resources.getInteger(R.integer.download_interval));
    }

    public JSONObject getJson() {
        return json;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(json.toString());
    }

    public static final Creator<SynchronizeConfig> CREATOR = new SynchronizeConfigCreator();

    public static List<SynchronizeConfig> load(InputStream in) throws JSONException {
        String data = convertStreamToString(in);
        return load(data);
    }

    public static List<SynchronizeConfig> load(String data) throws JSONException {
        JSONObject jsonConfigs = new JSONObject(data);

        ArrayList<SynchronizeConfig> configs = new ArrayList<>();

        Iterator keys = jsonConfigs.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            configs.add(new SynchronizeConfig(key, jsonConfigs.getJSONObject(key)));
        }

        return configs;
    }

    public static long randomID(String name) {
        int rnd = RANDOM.nextInt();
        int hash = name.hashCode();

        return rnd + hash;
    }

    public static void save(Iterable<SynchronizeConfig> configs, OutputStream fos) throws JSONException, IOException {
        JSONObject jsonConfigs = new JSONObject();

        for (SynchronizeConfig config : configs) {
            jsonConfigs.put(config.getName(), config.getJson());
        }

        fos.write(jsonConfigs.toString().getBytes("UTF-8"));
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static class SynchronizeConfigCreator implements Creator<SynchronizeConfig> {
        public SynchronizeConfig createFromParcel(Parcel in) {
            try {
                return new SynchronizeConfig(in.readString(), new JSONObject(in.readString()));
            } catch (JSONException e) {
                return new SynchronizeConfig("none");
            }
        }

        public SynchronizeConfig[] newArray(int size) {
            return new SynchronizeConfig[size];
        }
    }
}
