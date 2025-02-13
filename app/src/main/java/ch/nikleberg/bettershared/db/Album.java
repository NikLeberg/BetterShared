package ch.nikleberg.bettershared.db;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;

@Entity(tableName = "album")
public class Album {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "path")
    public String path;

    @ColumnInfo(name = "thumb")
    public byte[] thumb;

    @ColumnInfo(name = "count")
    public int count;

    @ColumnInfo(name = "drive_id")
    public String driveId;

    @ColumnInfo(name = "item_id")
    public String itemId;

    @Ignore
    public Album() {
        this(0, "", "", null, 0, "", "");
    }

    public Album(long id, String name, String path, byte[] thumb, int count, String driveId, String itemId) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.thumb = thumb;
        this.count = count;
        this.driveId = driveId;
        this.itemId = itemId;
    }

    @Ignore
    public boolean equals(@NonNull Album album) {
        return (id == album.id &&
                name.equals(album.name) &&
                path.equals(album.path) &&
                Arrays.equals(thumb, album.thumb) &&
                count == album.count &&
                driveId.equals(album.driveId) &&
                itemId.equals(album.itemId)
        );
    }
}
