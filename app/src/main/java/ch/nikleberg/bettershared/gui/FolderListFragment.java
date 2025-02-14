package ch.nikleberg.bettershared.gui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.microsoft.graph.serviceclient.GraphServiceClient;

import ch.nikleberg.bettershared.R;
import ch.nikleberg.bettershared.data.FolderRepository;
import ch.nikleberg.bettershared.databinding.FragmentFolderItemBinding;
import ch.nikleberg.bettershared.databinding.FragmentFolderListBinding;
import ch.nikleberg.bettershared.model.FolderListModel;
import ch.nikleberg.bettershared.ms.auth.Auth;
import ch.nikleberg.bettershared.ms.auth.AuthProvider;

public class FolderListFragment extends Fragment implements FolderRecyclerViewAdapter.FolderClickListener {

    private FolderListModel model;

    private FolderRecyclerViewAdapter adapter;
    private FragmentFolderListBinding binding;

    public FolderListFragment() {
        super(R.layout.fragment_folder_list);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new FolderRecyclerViewAdapter();
        adapter.setClickListener(this);

        Bundle args = getArguments();
        String folderId = null;
        if (null != args) folderId = args.getString("folder_id");
        if (null == folderId) folderId = "";

        GraphServiceClient graph = new GraphServiceClient(new AuthProvider(Auth.getInstance()));
        FolderRepository repo = new FolderRepository(graph);
        model = FolderListModel.Factory.build(getViewModelStore(), repo, folderId);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentFolderListBinding.bind(view);

        NavController navController = Navigation.findNavController(binding.getRoot());
        NavigationUI.setupWithNavController(binding.toolBar, navController);

        binding.folderRecycler.setAdapter(adapter);
        binding.progress.setIndeterminate(true);
        binding.refresh.setOnRefreshListener(() -> {
            binding.refresh.setRefreshing(false);
            binding.progress.setVisibility(View.VISIBLE);
            model.reload();
        });

        model.getFolders().observe(getViewLifecycleOwner(), folders -> {
            binding.progress.setVisibility(View.GONE);
            adapter.submitList(folders);
        });
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

    @Override
    public void onFolderClick(int position, FragmentFolderItemBinding binding) {
        Bundle args = new Bundle();
        args.putString("folder_id", adapter.get(position).id);
        Navigation.findNavController(this.binding.getRoot()).navigate(R.id.action_folderListFragment_self,
                args, null);
    }
}
