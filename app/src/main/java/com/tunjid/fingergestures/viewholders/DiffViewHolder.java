package com.tunjid.fingergestures.viewholders;

import android.transition.AutoTransition;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.DiffAdapter;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

import static android.transition.TransitionManager.beginDelayedTransition;
import static com.tunjid.fingergestures.App.nullCheck;

public abstract class DiffViewHolder<T extends DiffAdapter> extends AppViewHolder {

    private static final Map<String, Integer> sizeMap = new HashMap<>();

    DiffViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
    }

    public static void onActivityDestroyed() { sizeMap.clear(); }

    // The compiler error is odd, the type of the single should be independent of the types of the DiffAdapter
    @SuppressWarnings("unchecked")
    void diff() {
        nullCheck(getAdapter(), adapter -> disposables.add(adapter.calculateDiff().subscribe(this::onDiff, o -> {})));
    }

    abstract String getSizeCacheKey();

    @Nullable
    abstract T getAdapter();

    private void onDiff(Object value) {
        if (!(value instanceof Integer)) return;

        String key = getSizeCacheKey();
        @SuppressWarnings("ConstantConditions")
        int oldSize = sizeMap.getOrDefault(key, 0);
        int newSize = (int) value;

        if (oldSize != newSize) beginDelayedTransition((ViewGroup) itemView, new AutoTransition());
        sizeMap.put(key, newSize);
    }
}
