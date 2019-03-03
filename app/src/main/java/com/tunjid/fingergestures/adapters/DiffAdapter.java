package com.tunjid.fingergestures.adapters;

import com.tunjid.androidbootstrap.functions.collections.Lists;
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.androidbootstrap.recyclerview.diff.Diff;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

import static com.tunjid.fingergestures.App.backgroundToMain;

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

        disposables.add(backgroundToMain(this::getDiff)
                .subscribe(diff -> {
                    Lists.replace(list, diff.items);
                    subject.onNext(list.size()); // must be first
                    diff.result.dispatchUpdatesTo(this);
                }, Throwable::printStackTrace));

        return subject.firstOrError();
    }

    private Diff<S> getDiff() {
        return Diff.calculate(list,
                listSupplier.get(),
                (listCopy, newList) -> newList,
                item -> Differentiable.fromCharSequence(item::toString));
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        disposables.clear();
        super.onDetachedFromRecyclerView(recyclerView);
    }
}
