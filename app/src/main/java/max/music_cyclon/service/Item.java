package max.music_cyclon.service;

public class Item {

    private int id;
    private String path;
    private String artist;
    private String title;
    private String format;

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getPath() {
        if (path != null) {
            return path;
        } else {
            return "/" + artist + "/" + title + "_" + id + "." + format.toLowerCase();
        }
    }
    public String getArtist() {
        return artist;
    }
    public String getTitle() {
        return title;
    }
    public String getFormat() {
        return format;
    }

    public void setPath(String path) {
        this.path = path;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
}
