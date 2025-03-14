package ch.nikleberg.bettershared.data;


import android.provider.CloudMediaProviderContract;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "media",
        foreignKeys = {@ForeignKey(entity = Album.class,
                parentColumns = "_id",
                childColumns = "album_id",
                onDelete = ForeignKey.SET_NULL,
                onUpdate = ForeignKey.CASCADE)
        }
)
public class Media {
    @PrimaryKey(autoGenerate = true)
    public long _id = 0;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.SIZE_BYTES)
    public long size = 0;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.DATE_TAKEN_MILLIS)
    public long dateTaken = 0;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.MIME_TYPE)
    public String mimeType = null;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.HEIGHT)
    public long height = 0;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.WIDTH)
    public long width = 0;

    public static final int DEFAULT_ORIENTATION = 0;
    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.ORIENTATION, defaultValue = "" + DEFAULT_ORIENTATION)
    public int orientation = DEFAULT_ORIENTATION;

    public static final long DEFAULT_DURATION = 0;
    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.DURATION_MILLIS, defaultValue = "" + DEFAULT_DURATION)
    public long duration = DEFAULT_DURATION;

    @ColumnInfo(name = CloudMediaProviderContract.MediaColumns.SYNC_GENERATION)
    public long syncGeneration = 0;

    @ColumnInfo(name = "album_id", index = true)
    public Long albumId = 0L;

    @ColumnInfo(name = "drive_id")
    public String driveId = null;

    @ColumnInfo(name = "item_id", index = true)
    public String itemId = null;
}
