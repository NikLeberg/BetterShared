package ch.nikleberg.bettershared.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import java.util.List;

import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.data.AlbumRepository;

public class AlbumListModel extends ViewModel {
    private final AlbumRepository repo;
    private final LiveData<List<Album>> albums;

    public AlbumListModel(AlbumRepository repo) {
        this.repo = repo;
        albums = repo.getAlbums();
    }

    public LiveData<List<Album>> getAlbums() {
        return albums;
    }

    public void add(String folderId) {
        repo.add(folderId);
    }

    public void remove(Album album) {
        repo.remove(album);
    }

    public static class Factory {
        public static final CreationExtras.Key<AlbumRepository> REPOSITORY_KEY = new CreationExtras.Key<>() {
        };

        public static final ViewModelInitializer<AlbumListModel> initializer = new ViewModelInitializer<>(
                AlbumListModel.class,
                creationExtras -> {
                    AlbumRepository repo = creationExtras.get(REPOSITORY_KEY);
                    assert repo != null;
                    return new AlbumListModel(repo);
                }
        );

        public static AlbumListModel build(ViewModelStore owner, AlbumRepository repo) {
            MutableCreationExtras extras = new MutableCreationExtras();
            extras.set(REPOSITORY_KEY, repo);
            return new ViewModelProvider(owner, ViewModelProvider.Factory.from(initializer), extras)
                    .get(AlbumListModel.class);
        }
    }
}
