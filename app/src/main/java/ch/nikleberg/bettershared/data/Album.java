package ch.nikleberg.bettershared.data;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;
import java.util.Objects;

@Entity(tableName = "album")
public class Album {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "path")
    public String path;

    @ColumnInfo(name = "thumb")
    public byte[] thumb;

    @ColumnInfo(name = "count")
    public long count;

    @ColumnInfo(name = "drive_id")
    public String driveId;

    @ColumnInfo(name = "item_id")
    public String itemId;

    @Ignore
    public Album() {
        this(0, "", "", null, 0, "", "");
    }

    public Album(long id, String name, String path, byte[] thumb, long count, String driveId, String itemId) {
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
                Objects.equals(name, album.name) &&
                Objects.equals(path, album.path) &&
                Arrays.equals(thumb, album.thumb) &&
                count == album.count &&
                Objects.equals(driveId, album.driveId) &&
                Objects.equals(itemId, album.itemId)
        );
    }
}
