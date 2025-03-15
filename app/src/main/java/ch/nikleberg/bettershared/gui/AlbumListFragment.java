package ch.nikleberg.bettershared.gui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.List;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.data.AlbumRepository;
import ch.nikleberg.bettershared.databinding.FragmentAlbumItemBinding;
import ch.nikleberg.bettershared.databinding.FragmentAlbumListBinding;
import ch.nikleberg.bettershared.model.AlbumListModel;
import ch.nikleberg.bettershared.ms.DriveUtils;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;
import ch.nikleberg.bettershared.work.SyncManager;

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

        GraphServiceClient graph = new GraphServiceClient(new AuthProvider(
                Auth.getInstance(requireContext().getApplicationContext(), DriveUtils.DRIVE_SCOPES)));

        AlbumRepository repo = new AlbumRepository(requireContext(), graph);
        model = AlbumListModel.Factory.build(getViewModelStore(), repo);

        Bundle args = getArguments();
        String folderId = null;
        if (null != args) folderId = args.getString("new_album_folder_id");
        if (null != folderId) {
            addAndSync(folderId);
        }
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

        installItemSwipeHandler();
        installMenuOptions();
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

    private void installItemSwipeHandler() {
        ColorDrawable background = new ColorDrawable(Color.RED);
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (dX != 0f) {
                    View item = viewHolder.itemView;
                    background.setAlpha(255 * (int) Math.abs(dX) / item.getWidth());
                    background.setBounds(item.getLeft(), item.getTop(), item.getRight(), item.getBottom());
                    background.draw(c);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // should never be called
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                model.remove(adapter.get(viewHolder.getAdapterPosition()));
            }
        });
        helper.attachToRecyclerView(binding.albumRecycler);
    }

    private void installMenuOptions() {
        binding.toolBar.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_album_list, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_album_list_sync_now) {
                    triggerSync();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onAlbumClick(int position, FragmentAlbumItemBinding binding) {
        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(binding.albumName, "album_name")
                .addSharedElement(binding.albumThumb, "album_thumb")
                .build();
        Bundle args = new Bundle();
        args.putLong("album_id", adapter.get(position)._id);
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_albumListFragment_to_albumEditFragment,
                args, null, extras);
    }

    private void onFabClick(View view) {
        Bundle args = new Bundle();
        args.putString("folder_id", "");
        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_albumListFragment_to_folderListFragment,
                args, null);
    }

    private void addAndSync(String folderId) {
        // Trigger sync via LiveData observer, otherwise the asynchronously inserted album by the
        // model would not be (yet) visible when the SyncWorker runs. The observer immediately runs
        // on registration, only do any actual sync on the second invocation.
        Observer<List<Album>> observer = new Observer<>() {
            private int called = 0;

            @Override
            public void onChanged(List<Album> albums) {
                called++;
                if (2 <= called) {
                    triggerSync();
                    model.getAlbums().removeObserver(this);
                }
            }
        };
        model.getAlbums().observeForever(observer);
        model.add(folderId);
    }

    private void triggerSync() {
        SyncManager.getInstance(requireContext()).syncNow();
    }
}
