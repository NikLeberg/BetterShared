package ch.nikleberg.bettershared.gui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EmptyRecyclerViewObserver extends RecyclerView.AdapterDataObserver {
    private final RecyclerView recyclerView;
    private final View emptyView;
    private boolean isEmptyViewVisible;

    public EmptyRecyclerViewObserver(@NonNull RecyclerView recyclerView, @NonNull View emptyView, boolean initialVisible) {
        assert recyclerView.getAdapter() != null;
        this.recyclerView = recyclerView;
        this.emptyView = emptyView;
        this.isEmptyViewVisible = initialVisible;
        setEmptyVisibility(initialVisible);
        recyclerView.getAdapter().registerAdapterDataObserver(this);
    }

    private void updateVisibility() {
        assert recyclerView != null && recyclerView.getAdapter() != null && emptyView != null;
        boolean shouldShowEmptyView = recyclerView.getAdapter().getItemCount() == 0;
        if (shouldShowEmptyView != isEmptyViewVisible) {
            isEmptyViewVisible = shouldShowEmptyView;
            setEmptyVisibility(shouldShowEmptyView);
        }
    }

    private void setEmptyVisibility(boolean visible) {
        emptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onChanged() {
        updateVisibility();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        updateVisibility();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        updateVisibility();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        updateVisibility();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        updateVisibility();
    }
}
