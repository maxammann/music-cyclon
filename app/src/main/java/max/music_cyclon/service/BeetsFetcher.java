package max.music_cyclon.service;

import android.content.res.Resources;
import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import max.music_cyclon.SynchronizeConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BeetsFetcher {
    public static final Random RANDOM = new Random();

    private final String address;
    private final Resources resources;

    public BeetsFetcher(String address, Resources resources) {
        this.address = address;
        this.resources = resources;
    }

    public List<Item> fetch(SynchronizeConfig config) throws IOException {
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

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address + get)
                .build();

        Response response = client.newCall(request).execute();

        if (response.code() != 200) {
            Log.e("ERROR", "Server returned HTTP " + response.message());
            return Collections.emptyList();
        }


        InputStream stream = response.body().byteStream();
        List<Item> items = parseJson(stream, config.getSize(resources), config.isAlbum(resources));
        stream.close();

        return items;
    }

    private List<Item> parseJson(InputStream stream, int size, boolean isAlbums) throws IOException {
        JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(stream, "UTF-8")));

        List<Item> items = new ArrayList<>();
        List<ArrayList<Item>> albums = new ArrayList<>();

        reader.beginObject();
        String root = reader.nextName();
//        boolean isAlbums = root.equals("albums");
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

        // Select random
        if (isAlbums) {
            List<ArrayList<Item>> randomAlbums = selectRandom(albums, size);

            for (ArrayList<Item> album : randomAlbums) {
                items.addAll(album);
            }
            return Collections.unmodifiableList(items);
        } else {
            return selectRandom(items, size);
        }
    }

    public <T> List<T> selectRandom(List<T> list, int n) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<T> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(list.get(RANDOM.nextInt(list.size() - 1)));
        }

        return Collections.unmodifiableList(out);
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
                    item.setID(reader.nextInt());
                    break;
                case "path":
                    item.setPath(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return item;
    }

}
