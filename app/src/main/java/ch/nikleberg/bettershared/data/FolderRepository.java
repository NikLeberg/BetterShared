package ch.nikleberg.bettershared.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.nikleberg.bettershared.ms.DriveUtils;

public class FolderRepository {
    private final GraphServiceClient graph;
    private final Map<String, MutableLiveData<List<Folder>>> map = new HashMap<>();

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

        DriveUtils.getDrives(graph, DriveUtils.SELECT_DRIVES).thenAccept(drives -> {
            ArrayList<Folder> folders = new ArrayList<>();
            drives.forEach(drive ->
                    folders.add(new Folder(drive.getId() + "!root", drive.getDriveType(), -1))
            );
            map.get("").postValue(folders);
        });
    }

    private void loadFolder(String folderId) {
        assert null != map.get(folderId);

        String[] ids = Folder.splitId(folderId);
        DriveUtils.getDriveItems(graph, ids[0], ids[1], DriveUtils.SELECT_DRIVE_ITEMS_FOLDERS).thenAccept(items -> {
            ArrayList<Folder> folders = new ArrayList<>();
            items.forEach(item -> {
                if (null != item.getFolder())
                    folders.add(new Folder(item.getId(), item.getName(), item.getFolder().getChildCount()));
            });
            map.get(folderId).postValue(folders);
        });
    }
}