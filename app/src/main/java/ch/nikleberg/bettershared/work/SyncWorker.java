package ch.nikleberg.bettershared.work;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Image;
import com.microsoft.graph.models.Photo;
import com.microsoft.graph.models.Video;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.List;

import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.data.AlbumDao;
import ch.nikleberg.bettershared.data.Media;
import ch.nikleberg.bettershared.data.MediaDao;
import ch.nikleberg.bettershared.data.RoomsDB;
import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class SyncWorker extends Worker {
    private static final String TAG = SyncWorker.class.getSimpleName();

    private final AlbumDao albumDao;
    private final MediaDao mediaDao;
    private final GraphServiceClient graph;
    private long newSyncGeneration = 0;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        RoomsDB db = RoomsDB.getDatabase(context);
        albumDao = db.albumDao();
        mediaDao = db.mediaDao();
        graph = new GraphServiceClient(new AuthProvider(
                Auth.getInstance(context.getApplicationContext(), DriveUtils.DRIVE_SCOPES)));
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "starting sync of album medias");
        newSyncGeneration = mediaDao.getSyncGeneration() + 1;
        List<Album> albums = albumDao.getAlbums();
        for (Album album : albums) {
            Pair<List<DriveItem>, String> itemsAndDelta =
                    DriveUtils.getDriveItemsWithDelta(graph, album.driveId, album.itemId, album.deltaToken, DriveUtils.SELECT_DRIVE_ITEMS_DELTA);
            album.deltaToken = itemsAndDelta.second;
            albumDao.update(album);
            processItems(itemsAndDelta.first, album);
        }

        Log.d(TAG, "did the work");
        return Result.success();
    }

    private void processItems(List<DriveItem> items, Album album) {
        for (DriveItem item : items) {
            processItem(item, album);
        }
    }

    private void processItem(DriveItem item, Album album) {
        if (null != item.getFolder()) return;

        Image image = item.getImage();
        Video video = item.getVideo();
        if (null == image && null == video) return;

        Media media = mediaDao.getMediaByItemId(item.getId());
        if (null == media) {
            Log.d(TAG, "syncing new media: " + item.getId());
            media = new Media();
        } else {
            Log.d(TAG, "syncing updated media: " + item.getId() + " (id: " + media._id + ")");
        }

        media.size = item.getSize();
        media.dateTaken = item.getCreatedDateTime().toEpochSecond() * 1000;
        media.mimeType = item.getFile().getMimeType();
        if (null != image) {
            media.height = image.getHeight();
            media.width = image.getWidth();
        } else {
            media.height = video.getHeight();
            media.width = video.getWidth();
            media.duration = video.getDuration();
        }
        Photo photo = item.getPhoto();
        if (null != photo) {
            media.orientation = photo.getOrientation();
        }
        media.syncGeneration = newSyncGeneration;
        media.albumId = album._id;
        media.driveId = album.driveId;
        media.itemId = item.getId();

        if (0 == media._id) {
            mediaDao.insert(media);
        } else {
            mediaDao.update(media);
        }
    }
}
