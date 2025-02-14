package ch.nikleberg.bettershared.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import ch.nikleberg.bettershared.ms.DriveUtils;

public class FolderRepository {
    private final GraphServiceClient graph;
    private final Map<String, MutableLiveData<List<Folder>>> map = new HashMap<>();

    public static final ExecutorService executor = ForkJoinPool.commonPool();

    public FolderRepository(GraphServiceClient graph) {
        this.graph = graph;
    }

    public LiveData<List<Folder>> getFolders(@NonNull String folderId) {
        if (!map.containsKey(folderId))
            map.put(folderId, new MutableLiveData<>());

        if (folderId.isEmpty())
            loadDrives();
        else
            loadFolder(folderId);

        return map.get(folderId);
    }

    private void loadDrives() {
        assert null != map.get("");
        Log.d("FolderRepository", "loadDrives");

        executor.execute(() -> DriveUtils.getDrives(graph).thenAccept(drives -> {
            ArrayList<Folder> folders = new ArrayList<>();
            drives.forEach(drive ->
                folders.add(new Folder(drive.getId() + "!root", drive.getDriveType()))
            );
            map.get("").postValue(folders);
        }));
    }

    private void loadFolder(String folderId) {
        assert null != map.get(folderId);
        Log.d("FolderRepository", "loadFolder: " + folderId);

        executor.execute(() -> {
            String[] ids = folderId.split("!");
            String itemId = "root".equals(ids[1]) ? "root" : folderId;
            DriveUtils.getDriveItems(graph, ids[0], itemId).thenAccept(items -> {
                ArrayList<Folder> folders = new ArrayList<>();
                items.forEach(item -> {
                    if (null != item.getFolder())
                        folders.add(new Folder(item.getId(), item.getName()));
                });
                map.get(folderId).postValue(folders);
            });
        });
    }
}