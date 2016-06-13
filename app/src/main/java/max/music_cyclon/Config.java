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
import java.util.Scanner;

public class Config implements Parcelable {

    private final String name;
    private final JSONObject json;

    public Config(String name, JSONObject json) {
        this.name = name;
        this.json = json;
    }

    public Config(String name) {
        this(name, new JSONObject());
    }

    public String getName() {
        return name;
    }

    public int getSize(Resources resources) {
        return json.optInt("size", resources.getInteger(R.integer.size));
    }

    public boolean isRandom(Resources resources) {
        return json.optBoolean("size", resources.getBoolean(R.bool.random));
    }

    public boolean isAlbum(Resources resources) {
        return json.optBoolean("use_albums", resources.getBoolean(R.bool.use_albums));
    }

    public String getQuery(Resources resources) {
        return json.optString("query", resources.getString(R.string.query));
    }

    public boolean isStartCharging(Resources resources) {
        return json.optBoolean("start_charging", resources.getBoolean(R.bool.start_charging));
    }

    public int getDownloadInterval(Resources resources) {
        return json.optInt("download_interval", resources.getInteger(R.integer.download_interval));
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

    public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            try {
                return new Config(in.readString(), new JSONObject(in.readString()));
            } catch (JSONException e) {
                return new Config("none");
            }
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public static List<Config> load(InputStream in) throws JSONException {
        String data = convertStreamToString(in);
        return load(data);
    }

    public static List<Config> load(String data) throws JSONException {
        JSONObject jsonConfigs = new JSONObject(data);

        ArrayList<Config> configs = new ArrayList<>();

        Iterator keys = jsonConfigs.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            configs.add(new Config(key, jsonConfigs.getJSONObject(key)));
        }

        return configs;
    }

    public static void save(Iterable<Config> configs, OutputStream fos) throws JSONException, IOException {
        JSONObject jsonConfigs = new JSONObject();

        for (Config config : configs) {
            jsonConfigs.put(config.getName(), config.getJson());
        }

        fos.write(jsonConfigs.toString().getBytes("UTF-8"));
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
