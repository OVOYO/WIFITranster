package io.github.ovoyo.wifitranster;


public class Doc {

    private String path;

    private String name;

    private String size;

    private String type;

    private long modified;

    private String date;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Doc{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", type='" + type + '\'' +
                ", modified=" + modified +
                ", date='" + date + '\'' +
                '}';
    }
}
