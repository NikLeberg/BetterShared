package ch.nikleberg.bettershared.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.identity.client.IAccount;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class ProviderRepository {
    private static final String TAG = ProviderRepository.class.getSimpleName();
    private static final String KEY_MEDIA_COLLECTION_ID = "media_collection_id";
    private static final String DEFAULT_MEDIA_COLLECTION_ID = UUID.randomUUID().toString();
    private static final String UNKNOWN_ACCOUNT = "unknown";

    private final AlbumDao albumDao;
    private final MediaDao mediaDao;
    private final SharedPreferences prefs;
    private final Auth auth;
    private final GraphServiceClient graph;
    private final File cacheDir;
    private final CacheManager cacheManager;

    public ProviderRepository(@NonNull Context context) {
        RoomsDB db = RoomsDB.getDatabase(context);
        this.albumDao = db.albumDao();
        this.mediaDao = db.mediaDao();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.cacheDir = context.getCacheDir();
        this.cacheManager = new CacheManager(db.cacheDao());

        this.auth = Auth.getInstance(context.getApplicationContext(), DriveUtils.DRIVE_SCOPES);
        this.graph = new GraphServiceClient(new AuthProvider(auth));
        if (!auth.isAuthenticated()) {
            auth.authenticateSilent().thenAccept(v -> {
                Log.d(TAG, "Successfully authenticated silently");
            }).exceptionally(e -> {
                Log.e(TAG, "Failed to authenticate silently", e);
                return null;
            });
        }
    }

    public boolean isLoggedIn() {
        return auth.isAuthenticated();
    }

    public void loginWithTimeout(long timeout) throws TimeoutException {
        try {
            auth.authenticateSilent().get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getMediaCollectionId() {
        if (!prefs.contains(KEY_MEDIA_COLLECTION_ID)) {
            prefs.edit().putString(KEY_MEDIA_COLLECTION_ID, DEFAULT_MEDIA_COLLECTION_ID).apply();
            return DEFAULT_MEDIA_COLLECTION_ID;
        } else {
            return prefs.getString(KEY_MEDIA_COLLECTION_ID, DEFAULT_MEDIA_COLLECTION_ID);
        }
    }

    public long getSyncGeneration() {
        return mediaDao.getSyncGeneration();
    }

    public String getAccountName() {
        IAccount account = auth.getAuthenticatedUser();
        return (null == account) ? UNKNOWN_ACCOUNT : account.getUsername();
    }

    public Cursor getAlbums() {
        return albumDao.getAlbumsForCloudProvider();
    }

    public Cursor getMedias() {
        return mediaDao.getMedias();
    }

    public Cursor getMediasByAlbumId(String albumId) {
        return mediaDao.getMediasByAlbumId(CloudProviderViews.stringIdToLongId(albumId));
    }

    public Cursor getDeletedMedias() {
        return mediaDao.getDeletedMedias();
    }

    public AssetFileDescriptor openMediaPreview(String mediaId, int width, int height, CancellationSignal cancel) throws FileNotFoundException {
        Log.d(TAG, "openMediaPreview: " + mediaId + ", " + width + "x" + height);
        long id = CloudProviderViews.stringIdToLongId(mediaId);
        Media media = getMediaByIdOrThrow(id);

        File cached = cacheManager.get(media._id, width * height);
        if (null != cached) {
            return servePreview(cached);
        }

        String url = DriveUtils.getDriveItemThumbnailUrl(graph, media.driveId, media.itemId, width, height);
        URL urlObj;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            FileNotFoundException ex = new FileNotFoundException("Url '" + url + "'is invalid");
            ex.initCause(e);
            throw ex;
        }

        String filename = "preview_" + media._id + "_" + width + "x" + height;
        File file = new File(cacheDir, filename);
        try {
            downloadFile(urlObj, file, cancel);
        } catch (IOException e) {
            FileNotFoundException ex = new FileNotFoundException("Download of preview media failed");
            ex.initCause(e);
            throw ex;
        }
        cacheManager.put(media._id, width * height, file.getPath());

        return servePreview(file);
    }

    public ParcelFileDescriptor openMedia(String mediaId, CancellationSignal cancel) throws FileNotFoundException {
        Log.d(TAG, "openMedia: " + mediaId);
        long id = CloudProviderViews.stringIdToLongId(mediaId);
        Media media = getMediaByIdOrThrow(id);

        File cached = cacheManager.get(media._id, 0);
        if (null != cached) {
            return serveMedia(cached);
        }

        InputStream in = DriveUtils.getDriveItemContent(graph, media.driveId, media.itemId);
        String filename = "media_" + media._id;
        File file = new File(cacheDir, filename);

        try {
            downloadFile(in, file, cancel);
        } catch (IOException e) {
            FileNotFoundException ex = new FileNotFoundException("Download of media failed");
            ex.initCause(e);
            throw ex;
        }
        cacheManager.put(media._id, 0, file.getPath());

        return serveMedia(file);
    }

    private Media getMediaByIdOrThrow(long id) throws FileNotFoundException {
        Media media = mediaDao.getMediaById(id);
        if (null == media) {
            throw new FileNotFoundException("Media with id '" + id + "' not found");
        }
        return media;
    }

    private AssetFileDescriptor servePreview(File preview) throws FileNotFoundException {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(preview, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, preview.length());
    }

    private ParcelFileDescriptor serveMedia(File media) throws FileNotFoundException {
        return ParcelFileDescriptor.open(media, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private void downloadFile(URL url, File outputFile, @Nullable CancellationSignal cancel) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + connection.getResponseCode());
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            downloadFile(in, outputFile, cancel);
        } finally {
            if (null != connection) connection.disconnect();
        }
    }

    private void downloadFile(InputStream in, File outputFile, @Nullable CancellationSignal cancel) throws IOException {
        try (in; FileOutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096]; // 4kb
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                if (cancel != null && cancel.isCanceled()) {
                    if (!outputFile.delete()) {
                        Log.e(TAG, "Failed to delete file: " + outputFile.getPath());
                    }
                    Log.d(TAG, "Download canceled");
                    throw new IOException("Download canceled");
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}
