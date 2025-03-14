package ch.nikleberg.bettershared.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.Date;
import java.util.List;

@Dao
public interface CacheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Cache cache);

    @Delete
    int delete(Cache cache);

    @Transaction
    default Cache get(long mediaId, int dimension) {
        Cache cache = _get(mediaId, dimension);
        if (null == cache) return null;
        _updateLastAccessed(cache.id, (new Date()).getTime());
        return cache;
    }

    @Query("SELECT * FROM cache WHERE id NOT IN (SELECT id FROM cache ORDER BY last_accessed DESC LIMIT :limit)")
    List<Cache> getOldest(int limit);

    @Query("SELECT * FROM cache WHERE media_id = (:mediaId) AND dimension = (:dimension)")
    Cache _get(long mediaId, int dimension);

    @Query("UPDATE cache SET last_accessed = (:timestamp) WHERE id = (:id)")
    void _updateLastAccessed(long id, long timestamp);
}