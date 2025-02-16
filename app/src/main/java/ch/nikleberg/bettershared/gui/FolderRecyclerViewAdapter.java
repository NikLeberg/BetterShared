package ch.nikleberg.bettershared.gui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ch.nikleberg.bettershared.data.Folder;
import ch.nikleberg.bettershared.databinding.FragmentFolderItemBinding;

class FolderRecyclerViewAdapter extends ListAdapter<Folder, FolderRecyclerViewAdapter.ViewHolder> {
    private FolderClickListener clickListener;

    public FolderRecyclerViewAdapter() {
        super(FolderRecyclerViewAdapter.DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentFolderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(FolderRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentFolderItemBinding binding;

        public ViewHolder(FragmentFolderItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this::onFolderClick);
            binding.folderCard.setOnClickListener(this::onFolderClick);
        }

        public void bindTo(Folder folder) {
            binding.folderName.setText(folder.name);
            binding.folderCount.setText(folder.count >= 0 ? String.valueOf(folder.count) : "?");
        }

        private void onFolderClick(View view) {
            if (null != clickListener)
                clickListener.onFolderClick(getAdapterPosition(), binding);
        }
    }

    public Folder get(int position) {
        return getItem(position);
    }

    public void setClickListener(FolderClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface FolderClickListener {
        void onFolderClick(int position, FragmentFolderItemBinding binding);
    }

    public static final DiffUtil.ItemCallback<Folder> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Folder oldFolder, @NonNull Folder newFolder) {
            return oldFolder.id == newFolder.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Folder oldFolder, @NonNull Folder newFolder) {
            return oldFolder.equals(newFolder);
        }
    };
}
