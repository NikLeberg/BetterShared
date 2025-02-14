package ch.nikleberg.bettershared.model;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import java.util.List;

import ch.nikleberg.bettershared.data.Folder;
import ch.nikleberg.bettershared.data.FolderRepository;

public class FolderListModel extends ViewModel {
    private final FolderRepository repo;
    private final String folderId;
    private final LiveData<List<Folder>> folders;

    public FolderListModel(FolderRepository repo, String folderId) {
        this.repo = repo;
        this.folderId = folderId;
        folders = repo.getFolders(folderId);

        addCloseable(() -> Log.d("FolderListModel", "closing folder: " + this.folderId));
    }

    public LiveData<List<Folder>> getFolders() {
        return folders;
    }

    public void reload() {
        repo.getFolders(folderId);
    }

    public static class Factory {
        public static final CreationExtras.Key<FolderRepository> REPOSITORY_KEY = new CreationExtras.Key<>() {
        };
        public static final CreationExtras.Key<String> FOLDER_ID_KEY = new CreationExtras.Key<>() {
        };

        public static final ViewModelInitializer<FolderListModel> initializer = new ViewModelInitializer<>(
                FolderListModel.class,
                creationExtras -> {
                    FolderRepository repo = creationExtras.get(REPOSITORY_KEY);
                    assert repo != null;
                    String folderId = creationExtras.get(FOLDER_ID_KEY);
                    assert folderId != null;
                    return new FolderListModel(repo, folderId);
                }
        );

        public static FolderListModel build(ViewModelStore owner, FolderRepository repo, String folderId) {
            MutableCreationExtras extras = new MutableCreationExtras();
            extras.set(REPOSITORY_KEY, repo);
            extras.set(FOLDER_ID_KEY, folderId);
            return new ViewModelProvider(owner, ViewModelProvider.Factory.from(initializer), extras)
                    .get(FolderListModel.class);
        }
    }
}
