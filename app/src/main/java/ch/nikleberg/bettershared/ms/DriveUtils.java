package ch.nikleberg.bettershared.ms;

import android.util.Pair;

import androidx.annotation.Nullable;

import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveCollectionResponse;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.models.ThumbnailSet;
import com.microsoft.graph.models.ThumbnailSetCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.serialization.UntypedObject;
import com.microsoft.kiota.serialization.UntypedString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DriveUtils {
    public static final List<String> DRIVE_SCOPES = List.of("Files.Read.All");
    public static final List<String> SELECT_DRIVES = List.of("id", "driveType");
    public static final List<String> SELECT_DRIVE_ITEMS_FOLDERS = List.of("id", "name", "folder");
    public static final List<String> SELECT_DRIVE_ITEM_FOLDER = List.of("id", "name", "folder", "createdDateTime", "parentReference");
    public static final List<String> SELECT_DRIVE_ITEMS_FILES = List.of("id", "name", "file");
    public static final List<String> SELECT_DRIVE_ITEMS_DELTA = List.of("id", "name", "size", "createdDateTime", "folder", "file", "image", "video", "deleted");

    private DriveUtils() {
    }

    public static CompletableFuture<List<Drive>> getDrives(GraphServiceClient graph, @Nullable List<String> select) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().get(request -> {
            if (null != select) {
                assert request.queryParameters != null;
                request.queryParameters.select = select.toArray(new String[0]);
            }
        }), new ArrayList<>(), DriveCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<List<Drive>> getDrives(GraphServiceClient graph) {
        return getDrives(graph, null);
    }

    public static CompletableFuture<DriveItem> getDriveItem(GraphServiceClient graph, String driveId, String itemId, @Nullable List<String> select) {
        return GraphUtils.getAsync(() -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).get());
    }

    public static CompletableFuture<DriveItem> getDriveItem(GraphServiceClient graph, String driveId, String itemId) {
        return getDriveItem(graph, driveId, itemId, null);
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId, String itemId, @Nullable List<String> select) {
        return GraphUtils.getPagedAsync(graph, () -> graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).children().get(request -> {
            if (null != select) {
                assert request.queryParameters != null;
                request.queryParameters.select = select.toArray(new String[0]);
            }
        }), new ArrayList<>(), DriveItemCollectionResponse::createFromDiscriminatorValue);
    }

    public static CompletableFuture<List<DriveItem>> getDriveItems(GraphServiceClient graph, String driveId, String itemId) {
        return getDriveItems(graph, driveId, itemId, null);
    }

    public static Pair<List<DriveItem>, String> getDriveItemsWithDelta(GraphServiceClient graph, String driveId, String itemId, @Nullable String deltaToken, @Nullable List<String> select) {
        DriveItemItemRequestBuilder baseRequest = graph.drives().byDriveId(driveId).items().byDriveItemId(itemId);
        if (null == deltaToken) {
            return GraphUtils.getPagedWithDelta(graph, baseRequest.delta().get(request -> {
                if (null != select) {
                    assert request.queryParameters != null;
                    request.queryParameters.select = select.toArray(new String[0]);
                }
            }), new ArrayList<>(), DriveItemCollectionResponse::createFromDiscriminatorValue);
        } else {
            return GraphUtils.getPagedWithDelta(graph, baseRequest.deltaWithToken(deltaToken).get(request -> {
                if (null != select) {
                    assert request.queryParameters != null;
                    request.queryParameters.select = select.toArray(new String[0]);
                }
            }), new ArrayList<>(), DriveItemCollectionResponse::createFromDiscriminatorValue);
        }
    }

    public static String getDriveItemThumbnailUrl(GraphServiceClient graph, String driveId, String itemId, int width, int height) {
        String size = "c" + width + "x" + height + "_crop";
        ThumbnailSetCollectionResponse thumbnails =
                graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).thumbnails().get(request -> {
                    assert request.queryParameters != null;
                    request.queryParameters.select = new String[]{"id", size};
                });
        List<ThumbnailSet> thumbList;
        if ((null == thumbnails) || (null == (thumbList = thumbnails.getValue())) || (thumbList.isEmpty())) {
            return null;
        }
        UntypedObject obj = (UntypedObject) thumbList.get(0).getAdditionalData().get(size);
        UntypedString url;
        if ((null == obj) || (null == (url = (UntypedString) obj.getValue().get("url")))) {
            return null;
        } else {
            return url.getValue();
        }
    }

    public static InputStream getDriveItemContent(GraphServiceClient graph, String driveId, String itemId) {
        return graph.drives().byDriveId(driveId).items().byDriveItemId(itemId).content().get();
    }
}
