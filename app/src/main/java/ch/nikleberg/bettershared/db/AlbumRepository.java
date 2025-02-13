package ch.nikleberg.bettershared.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class AlbumRepository {
    private final AlbumDao dao;
    private final LiveData<List<Album>> albums;

    public static final ExecutorService executor = ForkJoinPool.commonPool();

    public AlbumRepository(Context context) {
        AlbumDatabase db = AlbumDatabase.getDatabase(context);
        dao = db.albumDao();
        albums = dao.getAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albums;
    }

    public LiveData<Album> getAlbum(long albumId) {
        return dao.getAlbum(albumId);
    }

    public void add(Album album) {
        executor.execute(() -> {
            long id = dao.insert(album);
            if (id == -1) {
                // TODO: forward error
            }
        });
    }

    public void change(Album album) {
        executor.execute(() -> {
            int rows = dao.update(album);
            if (rows == 0) {
                // TODO: forward error
            }
        });
    }

    public void remove(Album album) {
        executor.execute(() -> {
            int rows = dao.delete(album);
            if (rows == 0) {
                // TODO: forward error
            }
        });
    }
}