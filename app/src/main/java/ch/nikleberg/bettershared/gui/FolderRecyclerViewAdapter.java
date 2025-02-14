package ch.nikleberg.bettershared.gui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.nikleberg.bettershared.data.Folder;
import ch.nikleberg.bettershared.databinding.FragmentFolderItemBinding;

public class FolderRecyclerViewAdapter extends RecyclerView.Adapter<FolderRecyclerViewAdapter.ViewHolder> {
    private List<Folder> data = null;
    private FolderClickListener clickListener;

    FolderRecyclerViewAdapter() {
    }

    @NonNull
    @Override
    public FolderRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentFolderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Folder folder = data.get(position);
        holder.binding.folderName.setText(folder.name);
//        holder.binding.folderCount.setText(folder.count);
        holder.binding.folderCount.setText("0");
    }

    public Folder getFolder(int position) {
        return data.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentFolderItemBinding binding;

        public ViewHolder(FragmentFolderItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this::onFolderClick);
            binding.folderCard.setOnClickListener(this::onFolderClick);
        }

        private void onFolderClick(View view) {
            if (null != clickListener)
                clickListener.onFolderClick(getAdapterPosition(), binding);
        }
    }

    void setClickListener(FolderClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface FolderClickListener {
        void onFolderClick(int position, FragmentFolderItemBinding binding);
    }

    @Override
    public int getItemCount() {
        return (null == data) ? 0 : data.size();
    }

    public void setData(@NonNull List<Folder> newData) {
        if (null == data) {
            data = newData;
            notifyItemRangeInserted(0, data.size());
        } else {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new FolderDiffCallback(data, newData));
            data.clear();
            data.addAll(newData);
            diff.dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    Log.d("FolderRecyclerViewAdapter", "onInserted: pos=" + position + ", count=" + count);
                }
                @Override
                public void onRemoved(int position, int count) {
                    Log.d("FolderRecyclerViewAdapter", "onRemoved: pos=" + position + ", count=" + count);
                }
                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    Log.d("FolderRecyclerViewAdapter", "onMoved: from=" + fromPosition + ", to=" + toPosition);
                }
                @Override
                public void onChanged(int position, int count, @Nullable Object payload) {
                    Log.d("FolderRecyclerViewAdapter", "onChanged: pos=" + position + ", count=" + count);
                }
            });
            diff.dispatchUpdatesTo(this);
        }
    }

    static class FolderDiffCallback extends DiffUtil.Callback {

        private final List<Folder> oldData, newData;

        public FolderDiffCallback(List<Folder> oldData, List<Folder> newData) {
            this.oldData = oldData;
            this.newData = newData;
        }

        @Override
        public int getOldListSize() {
            Log.d("FolderDiffCallback", "getOldListSize: " + oldData.size());
            return oldData.size();
        }

        @Override
        public int getNewListSize() {
            Log.d("FolderDiffCallback", "getNewListSize: " + newData.size());
            return newData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            boolean state = oldData.get(oldItemPosition).id == newData.get(newItemPosition).id;
            Log.d("FolderDiffCallback", "areItemsTheSame: " + state);
            return state;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            boolean state = oldData.get(oldItemPosition).equals(newData.get(newItemPosition));
            Log.d("FolderDiffCallback", "areContentsTheSame: " + state);
            Log.d("FolderDiffCallback", "old_id: " + oldData.get(oldItemPosition).id + ", new_id: " + newData.get(newItemPosition).id);
            return state;
        }
    }
}
