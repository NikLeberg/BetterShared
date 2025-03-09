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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

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

    public ProviderRepository(@NonNull Context context) {
        RoomsDB db = RoomsDB.getDatabase(context);
        this.albumDao = db.albumDao();
        this.mediaDao = db.mediaDao();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.cacheDir = context.getCacheDir();

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

    public AssetFileDescriptor getMediaPreview(String mediaId, int width, int height, CancellationSignal cancel) throws FileNotFoundException {
        Log.d(TAG, "getMediaPreview: " + mediaId + ", " + width + "x" + height);
        long id = CloudProviderViews.stringIdToLongId(mediaId);
        Media media = getMediaByIdOrThrow(id);

        if (haveMatchingCachedPreview(media, width, height)) {
            if (cachedPreviewExists(media)) {
                return serveCachedPreview(media.previewAssetPath);
            } else {
                media.previewAssetPath = null;
                media.previewAssetDimension = 0;
                mediaDao.update(media);
            }
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

        if (shouldCachePreview(media)) {
            return downloadPreview(media, width, height, urlObj, cancel);
        } else {
            return streamPreview(urlObj, cancel);
        }
    }

    public ParcelFileDescriptor getMedia(String mediaId, CancellationSignal cancel) throws FileNotFoundException {
        Log.d(TAG, "getMedia: " + mediaId);
        long id = CloudProviderViews.stringIdToLongId(mediaId);
        Media media = getMediaByIdOrThrow(id);

        InputStream in = DriveUtils.getDriveItemContent(graph, media.driveId, media.itemId);
        try {
            return streamFile(in, cancel);
        } catch (IOException e) {
            FileNotFoundException ex = new FileNotFoundException("Streaming of media failed");
            ex.initCause(e);
            throw ex;
        }
    }

    private Media getMediaByIdOrThrow(long id) throws FileNotFoundException {
        Media media = mediaDao.getMediaById(id);
        if (null == media) {
            throw new FileNotFoundException("Media with id '" + id + "' not found");
        }
        return media;
    }

    private boolean haveMatchingCachedPreview(Media media, int width, int height) {
        return ((null != media)
                && (null != media.previewAssetPath)
                && (width * height == media.previewAssetDimension));
    }

    private boolean cachedPreviewExists(Media media) {
        return ((null != media) && ((new File(media.previewAssetPath)).exists()));
    }

    private AssetFileDescriptor serveCachedPreview(String path) throws FileNotFoundException {
        File f = new File(path);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, f.length());
    }

    private boolean shouldCachePreview(Media media) {
        return (null == media.previewAssetPath);
    }

    private AssetFileDescriptor downloadPreview(Media media, int width, int height, URL url, @Nullable CancellationSignal cancel) throws FileNotFoundException {
        String filename = "preview_" + media._id + "_" + width + "x" + height + ".jpeg";
        File file = new File(cacheDir, filename);
        try {
            downloadFile(url, file, cancel);
        } catch (IOException e) {
            FileNotFoundException ex = new FileNotFoundException("Download of preview media failed");
            ex.initCause(e);
            throw ex;
        }
        String path = file.getPath();
        media.previewAssetPath = path;
        media.previewAssetDimension = width * height;
        mediaDao.update(media);
        return serveCachedPreview(path);
    }

    private AssetFileDescriptor streamPreview(URL url, CancellationSignal cancel) throws FileNotFoundException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + connection.getResponseCode());
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            ParcelFileDescriptor pfd = streamFile(in, cancel);
            return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            FileNotFoundException ex = new FileNotFoundException("Streaming of preview media failed");
            ex.initCause(e);
            throw ex;
        }
    }

    private void downloadFile(URL url, File outputFile, @Nullable CancellationSignal cancel) throws IOException {
        HttpURLConnection connection = null;
        InputStream in = null;
        FileOutputStream out = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + connection.getResponseCode());
            }

            in = new BufferedInputStream(connection.getInputStream());
            out = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096]; // 4kb
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                if (cancel != null && cancel.isCanceled()) {
                    if (!outputFile.delete()) {
                        Log.e(TAG, "Failed to delete preview file: " + outputFile.getPath());
                    }
                    Log.d(TAG, "Download canceled");
                    throw new IOException("Download canceled");
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } finally {
            if (null != out) out.close();
            if (null != in) in.close();
            if (null != connection) connection.disconnect();
        }
    }

    private ParcelFileDescriptor streamFile(InputStream in, @Nullable CancellationSignal cancel) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        final ParcelFileDescriptor writeSide = pipe[1];

        new Thread(() -> {
            try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(writeSide); in) {
                byte[] buffer = new byte[4096]; // 4kb
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    if (cancel != null && cancel.isCanceled()) {
                        Log.d(TAG, "Streaming canceled");
                        return;
                    }
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error streaming preview", e);
            }
        }).start();

        return readSide;
    }
}
