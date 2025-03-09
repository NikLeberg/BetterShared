package ch.nikleberg.bettershared.data;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.time.OffsetDateTime;
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
        RoomsDB db = RoomsDB.getDatabase(context);
        this.dao = db.albumDao();
        this.albums = dao.getAlbumsAsLiveData();
        this.graph = graph;
    }

    public LiveData<List<Album>> getAlbums() {
        return albums;
    }

    public LiveData<Album> getAlbum(long albumId) {
        return dao.getAlbumAsLiveData(albumId);
    }

    public void add(String folderId) {
        assert null != graph;
        executor.execute(() -> {
            String[] ids = Folder.splitId(folderId);
            DriveUtils.getDriveItem(graph, ids[0], ids[1], DriveUtils.SELECT_DRIVE_ITEM_FOLDER).thenAccept(item -> {
                Album album = new Album();

                album.name = item.getName();
                album.driveId = ids[0];
                album.itemId = ids[1];

                OffsetDateTime dateTime = item.getCreatedDateTime();
                if (null != dateTime) {
                    album.dateTaken = dateTime.toEpochSecond() * 1000;
                }

                com.microsoft.graph.models.Folder folder = item.getFolder();
                Integer count;
                if ((null != folder) && (null != (count = folder.getChildCount()))) {
                    album.mediaCount = count;
                }

                ItemReference reference = item.getParentReference();
                if (null != reference) {
                    album.path = reference.getPath();
                }

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