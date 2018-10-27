package com.tunjid.fingergestures.adapters;

import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;
import com.tunjid.androidbootstrap.view.recyclerview.InteractiveViewHolder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;


public abstract class DiffAdapter<V extends InteractiveViewHolder<T>, T extends InteractiveAdapter.AdapterListener> extends InteractiveAdapter<V, T> {

    final List<String> list;
    private final Supplier<List<String>> listSupplier;

    private final CompositeDisposable disposables;

    DiffAdapter(Supplier<List<String>> listSupplier, T listener) {
        super(listener);
        setHasStableIds(true);
        this.list = new ArrayList<>();
        this.listSupplier = listSupplier;
        this.disposables = new CompositeDisposable();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

    public void calculateDiff() {
        List<String> newPackageNames = listSupplier.get();
        disposables.add(Single.fromCallable(() -> DiffUtil.calculateDiff(new DiffCallBack(list, newPackageNames)))
                .subscribe(diffResult -> {
                    list.clear();
                    list.addAll(newPackageNames);
                    diffResult.dispatchUpdatesTo(this);
                }, Throwable::printStackTrace));
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        disposables.clear();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private static class DiffCallBack extends DiffUtil.Callback {

        private final List<String> oldList;
        private final List<String> newList;

        DiffCallBack(List<String> oldList, List<String> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }
    }
}
