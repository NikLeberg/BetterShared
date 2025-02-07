package ch.nikleberg.bettershared.db;

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
    List<Album> getAll();

    @Query("SELECT * FROM album WHERE id = (:albumId)")
    Album getById(long albumId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Album album);

    @Update
    int update(Album album);

    @Delete
    int delete(Album album);

    @Query("DELETE FROM album")
    void deleteAll();
}