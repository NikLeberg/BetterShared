package ch.nikleberg.bettershared.gui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import ch.nikleberg.bettershared.data.Album;
import ch.nikleberg.bettershared.databinding.FragmentAlbumItemBinding;

class AlbumRecyclerViewAdapter extends ListAdapter<Album, AlbumRecyclerViewAdapter.ViewHolder> {
    private AlbumClickListener clickListener;

    public AlbumRecyclerViewAdapter() {
        super(AlbumRecyclerViewAdapter.DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentAlbumItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(AlbumRecyclerViewAdapter.ViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentAlbumItemBinding binding;

        public ViewHolder(FragmentAlbumItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this::onAlbumClick);
        }

        public void bindTo(Album album) {
            binding.albumName.setText(album.name);
            binding.albumPath.setText(album.path);
            binding.albumCount.setText("(" + album.mediaCount + ")");
//            if (null != album.thumb) binding.albumThumb.setImageBitmap(
//                    BitmapFactory.decodeByteArray(album.thumb, 0, album.thumb.length)
//            );
            // overwrite transition name, must be unique to allow return animation of shared elements
            binding.albumName.setTransitionName("album_name_" + album._id);
            binding.albumThumb.setTransitionName("album_thumb_" + album._id);
        }

        private void onAlbumClick(View view) {
            if (null != clickListener)
                clickListener.onAlbumClick(getAdapterPosition(), binding);
        }
    }

    public Album get(int position) {
        return getItem(position);
    }

    public void setClickListener(AlbumClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface AlbumClickListener {
        void onAlbumClick(int position, FragmentAlbumItemBinding binding);
    }

    public static final DiffUtil.ItemCallback<Album> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Album oldAlbum, @NonNull Album newAlbum) {
            return oldAlbum._id == newAlbum._id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Album oldAlbum, @NonNull Album newAlbum) {
            return oldAlbum.equals(newAlbum);
        }
    };
}

