package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.data.AlbumRepository;
import ch.nikleberg.bettershared.databinding.FragmentAlbumEditBinding;
import ch.nikleberg.bettershared.model.AlbumEditModel;

public class AlbumEditFragment extends Fragment {

    private AlbumEditModel model;
    private FragmentAlbumEditBinding binding;

    public AlbumEditFragment() {
        super(R.layout.fragment_album_edit);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Transition animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move);
        setSharedElementEnterTransition(animation);
        setSharedElementReturnTransition(animation);

        Bundle args = getArguments();
        long albumId = 0L;
        if (null != args) albumId = args.getLong("album_id");
        AlbumRepository repo = new AlbumRepository(requireContext(), null);
        model = AlbumEditModel.Factory.build(getViewModelStore(), repo, albumId);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAlbumEditBinding.bind(view);

        NavController navController = Navigation.findNavController(binding.getRoot());
        NavigationUI.setupWithNavController(binding.toolBar, navController);

        model.getAlbum().observe(getViewLifecycleOwner(), album -> {
            binding.albumName.setText(album.name);
//            if (null != album.thumb) binding.albumThumb.setImageBitmap(
//                    BitmapFactory.decodeByteArray(album.thumb, 0, album.thumb.length)
//            );
        });

        binding.albumThumb.setOnClickListener(v -> {
            model.setName("Töröö!");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
