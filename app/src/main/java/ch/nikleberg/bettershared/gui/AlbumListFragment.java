package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.databinding.FragmentAlbumItemBinding;
import ch.nikleberg.bettershared.databinding.FragmentAlbumListBinding;
import ch.nikleberg.bettershared.db.Album;

public class AlbumListFragment extends Fragment implements AlbumRecyclerViewAdapter.AlbumClickListener {

    private FragmentAlbumListBinding binding;
    private AlbumRecyclerViewAdapter adapter;
    private ArrayList<Album> albums;

    public AlbumListFragment() {
        super(R.layout.fragment_album_list);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("AlbumListFragment", "onCreate");
        super.onCreate(savedInstanceState);

        setSharedTransition();

        albums = new ArrayList<>();
        albums.add(new Album(1, "Album 1", "path/to/drive/item", null, 10, "drive-id", "item-id"));
        albums.add(new Album(2, "Weltreise", "a/different/path/maybe", null, 5, "drive-id", "item-id"));
        albums.add(new Album(3, "Album 3", "3", null, 3, "drive-id", "item-id"));
        albums.add(new Album(4, "Album 4", "4", null, 3, "drive-id", "item-id"));
        albums.add(new Album(5, "Album 5", "5", null, 3, "drive-id", "item-id"));
        albums.add(new Album(6, "Album 6", "6", null, 3, "drive-id", "item-id"));
        albums.add(new Album(7, "Album 7", "7", null, 3, "drive-id", "item-id"));
        albums.add(new Album(8, "Album 8", "8", null, 3, "drive-id", "item-id"));
        albums.add(new Album(9, "Album 9", "9", null, 3, "drive-id", "item-id"));
        albums.add(new Album(10, "Album 10", "10", null, 3, "drive-id", "item-id"));
        albums.add(new Album(11, "Album 11", "11", null, 3, "drive-id", "item-id"));
        albums.add(new Album(12, "Album 12", "12", null, 3, "drive-id", "item-id"));
        albums.add(new Album(13, "Album 13", "13", null, 3, "drive-id", "item-id"));
        albums.add(new Album(14, "Album 14", "14", null, 3, "drive-id", "item-id"));
    }

    private void setSharedTransition() {
        Transition animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move);
        setSharedElementEnterTransition(animation);
        setSharedElementReturnTransition(animation);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.d("AlbumListFragment", "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAlbumListBinding.bind(view);

        fixupSharedTransition(view);

        adapter = new AlbumRecyclerViewAdapter();
        adapter.setData(albums);
        adapter.setClickListener(this);
        binding.albumRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.albumRecycler.setAdapter(adapter);
    }

    private void fixupSharedTransition(View view) {
        // delay transition until recycler view could load and create all views
        // source: https://developer.android.com/guide/fragments/animate#recyclerview
        postponeEnterTransition();
        final ViewGroup parentView = (ViewGroup) view.getParent();
        parentView.getViewTreeObserver()
                .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        parentView.getViewTreeObserver()
                                .removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }

    @Override
    public void onDestroyView() {
        Log.d("AlbumListFragment", "onDestroyView");
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    @Override
    public void onAlbumClick(int position, FragmentAlbumItemBinding binding) {
        Log.d("AlbumListFragment", "onAlbumClick");
        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(binding.albumName, "album_name")
                .addSharedElement(binding.albumThumb, "album_thumb")
                .build();
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_albumListFragment_to_albumEditFragment,
                null,
                null,
                extras);
    }
}
