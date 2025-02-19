package ch.nikleberg.bettershared.data;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Folder {
    // "drive-id" + "!" + "drive-item-id"
    // "AAAAAAAAAAAAAAA!XXXXX"
    public String id;

    public String name;
    public long count;

    public Folder(String id, String name, long count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public boolean equals(@NonNull Folder folder) {
        return (Objects.equals(id, folder.id) &&
                Objects.equals(name, folder.name) &&
                count == folder.count
        );
    }

    public String[] splitId() {
        return splitId(id);
    }

    public static String[] splitId(String id) {
        String[] ids = id.split("!");
        String itemId = "root".equals(ids[1]) ? "root" : id;
        return new String[]{ids[0], itemId};
    }
}
