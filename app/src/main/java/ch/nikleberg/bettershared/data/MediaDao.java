package ch.nikleberg.bettershared.data;

import android.database.Cursor;
import android.provider.CloudMediaProviderContract;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface MediaDao {
    @Query("SELECT * FROM mediaview WHERE album_id IS NOT NULL")
    Cursor getMedias();

    @Query("SELECT * FROM mediaview WHERE album_id = (:albumId)")
    Cursor getMediasByAlbumId(long albumId);

    @Query("SELECT * FROM media WHERE _id = (:mediaId)")
    Media getMediaById(long mediaId);

    @Query("SELECT * FROM media WHERE item_id = (:itemId)")
    Media getMediaByItemId(String itemId);

    @Query("SELECT MAX(sync_generation) FROM media")
    int getSyncGeneration();

    @Query("SELECT " + CloudMediaProviderContract.MediaColumns.ID + " FROM mediaview WHERE album_id IS NULL")
    Cursor getDeletedMedias();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Media media);

    @Update
    int update(Media media);
}