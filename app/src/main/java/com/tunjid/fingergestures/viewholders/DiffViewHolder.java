package com.tunjid.fingergestures.viewholders;

import android.transition.AutoTransition;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.adapters.AppAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import androidx.recyclerview.widget.DiffUtil;

import static android.transition.TransitionManager.beginDelayedTransition;

public abstract class DiffViewHolder<T> extends AppViewHolder {

    private static final Map<String, Integer> sizeMap = new HashMap<>();

    protected final List<T> items;
    private final ListManager<?, Void> listManager;

    DiffViewHolder(View itemView, List<T> items, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
        this.items = items;
        listManager = createListManager(itemView);
    }

    public static void onActivityDestroyed() { sizeMap.clear(); }

    final void diff() {
        disposables.add(App.diff(items, getListSupplier(), this::diffHash).subscribe(this::onDiff, Throwable::printStackTrace));
    }

    protected String diffHash(T item) { return item.toString(); }

    abstract String getSizeCacheKey();

    abstract Supplier<List<T>> getListSupplier();

    abstract ListManager<?, Void> createListManager(View itemView);

    private void onDiff(DiffUtil.DiffResult diffResult) {
        String key = getSizeCacheKey();
        @SuppressWarnings("ConstantConditions")
        int oldSize = sizeMap.getOrDefault(key, 0);
        int newSize = items.size();

        if (oldSize != newSize) beginDelayedTransition((ViewGroup) itemView, new AutoTransition());
        sizeMap.put(key, newSize);

        listManager.onDiff(diffResult);
    }
}
