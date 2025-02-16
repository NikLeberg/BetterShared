package ch.nikleberg.bettershared.data;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import ch.nikleberg.bettershared.ms.DriveUtils;

public class AlbumRepository {
    private final AlbumDao dao;
    private final LiveData<List<Album>> albums;
    private final GraphServiceClient graph;

    public static final ExecutorService executor = ForkJoinPool.commonPool();

    public AlbumRepository(Context context, @Nullable GraphServiceClient graph) {
        AlbumDatabase db = AlbumDatabase.getDatabase(context);
        this.dao = db.albumDao();
        this.albums = dao.getAlbums();
        this.graph = graph;
    }

    public LiveData<List<Album>> getAlbums() {
        return albums;
    }

    public LiveData<Album> getAlbum(long albumId) {
        return dao.getAlbum(albumId);
    }

    public void add(String folderId) {
        assert null != graph;
        executor.execute(() -> {
            String[] ids = Folder.splitId(folderId);
            DriveUtils.getDriveItem(graph, ids[0], ids[1], DriveUtils.SELECT_DRIVE_ITEM_FOLDER).thenAccept(item -> {
                Album album = new Album(0, item.getName(), item.getParentReference().getPath(), null, item.getFolder().getChildCount(), ids[0], ids[1]);
                long id = dao.insert(album);
                if (id == -1) {
                    // TODO: forward error
                }
            });
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