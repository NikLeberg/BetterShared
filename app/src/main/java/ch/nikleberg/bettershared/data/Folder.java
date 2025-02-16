package ch.nikleberg.bettershared.data;

import androidx.annotation.NonNull;

public class Folder {
    // "drive-id" + "!" + "drive-item-id"
    // "AAAAAAAAAAAAAAA!XXXXX"
    public String id;

    public String name;
    public int count;

    public Folder(String id, String name, int count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public boolean equals(@NonNull Folder folder) {
        return (id.equals(folder.id) &&
                name.equals(folder.name) &&
                count == folder.count
        );
    }
}
