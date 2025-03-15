package ch.nikleberg.bettershared.data;

import android.provider.CloudMediaProviderContract;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "album")
public class Album {
    @PrimaryKey(autoGenerate = true)
    public long _id = 0L;

    @ColumnInfo(name = CloudMediaProviderContract.AlbumColumns.DISPLAY_NAME)
    public String name = null;

    @ColumnInfo(name = CloudMediaProviderContract.AlbumColumns.DATE_TAKEN_MILLIS)
    public long dateTaken = 0L;

    @ColumnInfo(name = CloudMediaProviderContract.AlbumColumns.MEDIA_COUNT)
    public long mediaCount = 0L;

    @ColumnInfo(name = CloudMediaProviderContract.AlbumColumns.MEDIA_COVER_ID)
    public String mediaCoverId = null;

    @ColumnInfo(name = "path")
    public String path = null;

    @ColumnInfo(name = "drive_id")
    public String driveId = null;

    @ColumnInfo(name = "item_id")
    public String itemId = null;

    @ColumnInfo(name = "delta_token")
    public String deltaToken = null;

    @Ignore
    public boolean equals(@NonNull Album album) {
        return (_id == album._id &&
                Objects.equals(name, album.name) &&
                dateTaken == album.dateTaken &&
                mediaCount == album.mediaCount &&
                Objects.equals(mediaCoverId, album.mediaCoverId) &&
                Objects.equals(path, album.path) &&
                Objects.equals(driveId, album.driveId) &&
                Objects.equals(itemId, album.itemId) &&
                Objects.equals(deltaToken, album.deltaToken)
        );
    }
}
