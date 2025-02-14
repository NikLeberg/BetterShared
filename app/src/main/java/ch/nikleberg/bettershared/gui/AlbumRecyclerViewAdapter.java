package ch.nikleberg.bettershared.gui;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.nikleberg.bettershared.databinding.FragmentAlbumItemBinding;
import ch.nikleberg.bettershared.data.Album;

public class AlbumRecyclerViewAdapter extends RecyclerView.Adapter<AlbumRecyclerViewAdapter.ViewHolder> {
    private List<Album> data = null;
    private AlbumClickListener clickListener;

    AlbumRecyclerViewAdapter() {
    }

    @NonNull
    @Override
    public AlbumRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentAlbumItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Album album = data.get(position);
        holder.binding.albumName.setText(album.name);
        holder.binding.albumPath.setText(album.path);
        holder.binding.albumCount.setText("(" + album.count + ")");
        if (null != album.thumb) holder.binding.albumThumb.setImageBitmap(
                BitmapFactory.decodeByteArray(album.thumb, 0, album.thumb.length)
        );
        // overwrite transition name, must be unique to allow return animation of shared elements
        holder.binding.albumName.setTransitionName("album_name_" + album.id);
        holder.binding.albumThumb.setTransitionName("album_thumb_" + album.id);
    }

    public Album getAlbum(int position) {
        return data.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentAlbumItemBinding binding;

        public ViewHolder(FragmentAlbumItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this::onAlbumClick);
        }

        private void onAlbumClick(View view) {
            if (null != clickListener)
                clickListener.onAlbumClick(getAdapterPosition(), binding);
        }
    }

    void setClickListener(AlbumClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface AlbumClickListener {
        void onAlbumClick(int position, FragmentAlbumItemBinding binding);
    }

    @Override
    public int getItemCount() {
        return (null == data) ? 0 : data.size();
    }

    public void setData(@NonNull List<Album> newData) {
        if (null == data) {
            data = newData;
            notifyItemRangeInserted(0, data.size());
        } else {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new AlbumDiffCallback(data, newData));
            data.clear();
            data.addAll(newData);
            diff.dispatchUpdatesTo(this);
        }
    }

    static class AlbumDiffCallback extends DiffUtil.Callback {

        private final List<Album> oldData, newData;

        public AlbumDiffCallback(List<Album> oldData, List<Album> newData) {
            this.oldData = oldData;
            this.newData = newData;
        }

        @Override
        public int getOldListSize() {
            return oldData.size();
        }

        @Override
        public int getNewListSize() {
            return newData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldData.get(oldItemPosition).id == newData.get(newItemPosition).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldData.get(oldItemPosition).equals(newData.get(newItemPosition));
        }
    }
}
