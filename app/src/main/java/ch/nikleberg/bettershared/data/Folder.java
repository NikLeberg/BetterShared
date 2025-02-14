package ch.nikleberg.bettershared.data;

import androidx.annotation.NonNull;

public class Folder {
    // "drive-id" + "!" + "drive-item-id"
    // "AAAAAAAAAAAAAAA!XXXXX"
    public String id;

    public String name;

    public Folder(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean equals(@NonNull Folder folder) {
        return (id.equals(folder.id) &&
                name.equals(folder.name)
        );
    }
}
