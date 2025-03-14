package ch.nikleberg.bettershared.data;

import android.util.Log;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class CacheManager {
    private static final String TAG = CacheManager.class.getSimpleName();
    private static final int MAX_CACHE_SIZE = 100;

    private final CacheDao dao;
    private final ExecutorService executor = ForkJoinPool.commonPool();

    public CacheManager(CacheDao dao) {
        this.dao = dao;
    }

    public File get(long mediaId, int dimension) {
        Cache cache = dao.get(mediaId, dimension);
        if (null == cache) return null;
        File file = new File(cache.path);
        if (file.exists()) return file;
        dao.delete(cache);
        return null;
    }

    public void put(long mediaId, int dimension, String path) {
        Cache cache = new Cache();
        cache.mediaId = mediaId;
        cache.dimension = dimension;
        cache.path = path;
        cache.lastAccessed = (new Date()).getTime();
        dao.insert(cache);
        executor.execute(this::clean);
    }

    private void clean() {
        List<Cache> caches = dao.getOldest(MAX_CACHE_SIZE);
        for (Cache cache : caches) {
            Log.d(TAG, "Deleting cache: " + cache.path + " (id: " + cache.id + ")");
            dao.delete(cache);
            (new File(cache.path)).delete();
        }
    }
}
