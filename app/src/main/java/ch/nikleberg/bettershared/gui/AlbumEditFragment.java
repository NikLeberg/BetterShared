package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.databinding.FragmentAlbumEditBinding;

public class AlbumEditFragment extends Fragment {

    private FragmentAlbumEditBinding binding;

    public AlbumEditFragment() {
        super(R.layout.fragment_album_edit);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("AlbumEditFragment", "onCreate");
        super.onCreate(savedInstanceState);
        Transition animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move);
        setSharedElementEnterTransition(animation);
        setSharedElementReturnTransition(animation);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.d("AlbumEditFragment", "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAlbumEditBinding.bind(view);
    }

    @Override
    public void onDestroyView() {
        Log.d("AlbumEditFragment", "onDestroyView");
        super.onDestroyView();
        binding = null;
    }
}
