package ch.nikleberg.bettershared.data;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlbumDao {
    @Query("SELECT * FROM album")
    LiveData<List<Album>> getAlbumsAsLiveData();

    @Query("SELECT * FROM album")
    List<Album> getAlbums();

    @Query("SELECT * FROM albumview")
    Cursor getAlbumsForCloudProvider();

    @Query("SELECT * FROM album WHERE _id = (:albumId)")
    LiveData<Album> getAlbumAsLiveData(long albumId);

    @Query("SELECT * FROM album WHERE _id = (:albumId)")
    Album getAlbum(String albumId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Album album);

    @Update
    int update(Album album);

    @Delete
    int delete(Album album);
}