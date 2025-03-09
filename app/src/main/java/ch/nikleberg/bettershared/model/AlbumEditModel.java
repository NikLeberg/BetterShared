package ch.nikleberg.bettershared.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.data.AlbumRepository;

public class AlbumEditModel extends ViewModel {
    private final AlbumRepository repo;
    private final LiveData<Album> album;

    public AlbumEditModel(@NonNull AlbumRepository repo, long albumId) {
        this.repo = repo;
        this.album = repo.getAlbum(albumId);
    }

    public LiveData<Album> getAlbum() {
        return album;
    }

//    @Override
//    public void onCleared() {
//        if (-1 == album.id) {
//            repo.remove(album);
//        } else if (0 == album.id) {
//            repo.add(album);
//        } else {
//            repo.change(album);
//        }
//    }

    public void setName(String name) {
        Album a = album.getValue();
        a.name = name;
        repo.change(a);
    }

    public static class Factory {
        public static final CreationExtras.Key<AlbumRepository> REPOSITORY_KEY = new CreationExtras.Key<>() {
        };
        public static final CreationExtras.Key<Long> ALBUM_ID_KEY = new CreationExtras.Key<>() {
        };

        public static final ViewModelInitializer<AlbumEditModel> initializer = new ViewModelInitializer<>(
                AlbumEditModel.class,
                creationExtras -> {
                    AlbumRepository repo = creationExtras.get(REPOSITORY_KEY);
                    assert repo != null;
                    Long albumId = creationExtras.get(ALBUM_ID_KEY);
                    if (null == albumId) albumId = 0L;
                    return new AlbumEditModel(repo, albumId);
                }
        );

        public static AlbumEditModel build(ViewModelStore owner, AlbumRepository repo, long albumId) {
            MutableCreationExtras extras = new MutableCreationExtras();
            extras.set(REPOSITORY_KEY, repo);
            extras.set(ALBUM_ID_KEY, albumId);
            return new ViewModelProvider(owner, ViewModelProvider.Factory.from(initializer), extras)
                    .get(AlbumEditModel.class);
        }
    }
}
