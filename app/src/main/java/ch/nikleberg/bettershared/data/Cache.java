package ch.nikleberg.bettershared.data;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "cache",
        foreignKeys = {@ForeignKey(entity = Media.class,
                parentColumns = "_id",
                childColumns = "media_id",
                onDelete = ForeignKey.SET_NULL,
                onUpdate = ForeignKey.CASCADE)
        }
)
public class Cache {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;

    @ColumnInfo(name = "media_id", index = true)
    public Long mediaId = 0L;

    @ColumnInfo(name = "path")
    public String path = null;

    @ColumnInfo(name = "dimension")
    public int dimension = 0; // width * height of image, dimension of 0 means original image

    @ColumnInfo(name = "last_accessed")
    public long lastAccessed = 0L;
}
