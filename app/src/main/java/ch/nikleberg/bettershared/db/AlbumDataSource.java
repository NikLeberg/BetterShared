package ch.nikleberg.bettershared.db;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class AlbumDataSource {
    private final AlbumDao albumDao;
    private static final ArrayList<AlbumObserver> observers = new ArrayList<>();

    public enum Error {
        ADD_ALBUM_FAILED,
        UPDATE_ALBUM_FAILED,
        DELETE_ALBUM_FAILED
    }

    public static final ExecutorService executor = ForkJoinPool.commonPool();

    public AlbumDataSource(Context context) {
        AlbumDatabase db = AlbumDatabase.getDatabase(context);
        albumDao = db.albumDao();
    }

    public AlbumDataSource(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void addObserver(AlbumObserver observer) {
        observers.add(observer);
        // On initial addition as observer, send all available albums to the observer.
        executor.execute(() -> {
            List<Album> rules = albumDao.getAll();
            observer.onAlbumLoad(rules);
        });
    }

    public void removeObserver(AlbumObserver observer) {
        observers.remove(observer);
    }

    public void add(AlbumObserver observer, Album album) {
        executor.execute(() -> {
            // If an error occurs during the addition (insert returns row id -1) then notify the
            // triggering observer. On success notify all observers.
            long id = albumDao.insert(album);
            if (id == -1) {
                observer.onAlbumError(Error.ADD_ALBUM_FAILED, album);
            } else {
                album.id = id;
                for (AlbumObserver o : observers) {
                    o.onAlbumAdded(album);
                }
            }
        });
    }

    public void change(AlbumObserver observer, Album album) {
        executor.execute(() -> {
            // If an error occurs during the change (update returns count of affected rows) then
            // notify the triggering observer. On success notify all observers.
            int rows = albumDao.update(album);
            if (rows == 0) {
                observer.onAlbumError(Error.UPDATE_ALBUM_FAILED, album);
            } else {
                for (AlbumObserver o : observers) {
                    o.onAlbumChanged(album);
                }
            }
        });
    }

    public void remove(AlbumObserver observer, Album album) {
        executor.execute(() -> {
            // If an error occurs during the removal (delete returns count of affected rows) then
            // notify the triggering observer. On success notify all observers.
            int rows = albumDao.delete(album);
            if (rows == 0) {
                observer.onAlbumError(Error.DELETE_ALBUM_FAILED, album);
            } else {
                for (AlbumObserver o : observers) {
                    o.onAlbumRemoved(album);
                }
            }
        });
    }
}