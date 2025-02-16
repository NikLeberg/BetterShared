package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.data.AlbumRepository;
import ch.nikleberg.bettershared.databinding.FragmentAlbumItemBinding;
import ch.nikleberg.bettershared.databinding.FragmentAlbumListBinding;
import ch.nikleberg.bettershared.model.AlbumListModel;
import ch.nikleberg.bettershared.ms.GraphUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class AlbumListFragment extends Fragment implements AlbumRecyclerViewAdapter.AlbumClickListener {

    private AlbumListModel model;

    private AlbumRecyclerViewAdapter adapter;
    private FragmentAlbumListBinding binding;

    public AlbumListFragment() {
        super(R.layout.fragment_album_list);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSharedTransition();

        adapter = new AlbumRecyclerViewAdapter();
        adapter.setClickListener(this);

        GraphServiceClient graph = GraphUtils.Factory.getDebugServiceClient(new AuthProvider(Auth.getInstance()));

        AlbumRepository repo = new AlbumRepository(requireContext(), graph);
        model = AlbumListModel.Factory.build(getViewModelStore(), repo);

        Bundle args = getArguments();
        String folderId = null;
        if (null != args) folderId = args.getString("new_album_folder_id");
        if (null != folderId) model.add(folderId);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAlbumListBinding.bind(view);

        NavController navController = Navigation.findNavController(binding.getRoot());
        NavigationUI.setupWithNavController(binding.toolBar, navController);

        delaySharedTransition(view);

        binding.albumRecycler.setAdapter(adapter);
        binding.fab.setOnClickListener(this::onFabClick);

        new EmptyRecyclerViewObserver(binding.albumRecycler, binding.emptyView, true);

        model.getAlbums().observe(getViewLifecycleOwner(), albums -> adapter.submitList(albums));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adapter = null;
        // model = null; must this be done?
    }

    private void setSharedTransition() {
        Transition animation = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move);
        setSharedElementEnterTransition(animation);
        setSharedElementReturnTransition(animation);
    }

    private void delaySharedTransition(View view) {
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
    public void onAlbumClick(int position, FragmentAlbumItemBinding binding) {
        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(binding.albumName, "album_name")
                .addSharedElement(binding.albumThumb, "album_thumb")
                .build();
        Bundle args = new Bundle();
        args.putLong("album_id", adapter.get(position).id);
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_albumListFragment_to_albumEditFragment,
                args, null, extras);
    }

    private void onFabClick(View view) {
        Bundle args = new Bundle();
        args.putString("folder_id", "");
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_albumListFragment_to_folderListFragment,
                args, null);
    }
}
