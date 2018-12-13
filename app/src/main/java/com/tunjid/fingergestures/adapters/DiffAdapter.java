package com.tunjid.fingergestures.adapters;

import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;
import com.tunjid.androidbootstrap.view.recyclerview.InteractiveViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;


public abstract class DiffAdapter<V extends InteractiveViewHolder<T>, T extends InteractiveAdapter.AdapterListener, S>
        extends InteractiveAdapter<V, T> {

    final List<S> list;
    private final Supplier<List<S>> listSupplier;

    private final CompositeDisposable disposables;

    DiffAdapter(Supplier<List<S>> listSupplier, T listener) {
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

    public Single<Integer> calculateDiff() {
        PublishSubject<Integer> subject = PublishSubject.create();
        AtomicReference<List<S>> ref = new AtomicReference<>();
        disposables.add(Single.fromCallable(() -> DiffUtil.calculateDiff(new DiffCallBack<>(list, ref.updateAndGet(s -> listSupplier.get()))))
                .subscribeOn(io())
                .observeOn(mainThread())
                .subscribe(diffResult -> {
                    list.clear();
                    list.addAll(ref.get());
                    subject.onNext(list.size()); // must be first
                    diffResult.dispatchUpdatesTo(this);
                }, Throwable::printStackTrace));
        return subject.firstOrError();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        disposables.clear();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    private static class DiffCallBack<S> extends DiffUtil.Callback {

        private final List<S> oldList;
        private final List<S> newList;

        DiffCallBack(List<S> oldList, List<S> newList) {
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
