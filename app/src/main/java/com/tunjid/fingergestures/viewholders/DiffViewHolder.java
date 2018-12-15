package com.tunjid.fingergestures.viewholders;

import android.transition.AutoTransition;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.adapters.DiffAdapter;

import androidx.annotation.Nullable;

import static android.transition.TransitionManager.beginDelayedTransition;

abstract class DiffViewHolder<T extends DiffAdapter> extends AppViewHolder {

    DiffViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
    }

    // The compiler error is odd, the type of the single should be independent of the types of the DiffAdapter
    @SuppressWarnings("unchecked")
    void diff() {
        T adapter = getAdapter();
        if (adapter == null) return;

        disposables.add(adapter.calculateDiff()
                .subscribe(ignored -> beginDelayedTransition((ViewGroup) itemView, new AutoTransition()), o -> {}));
    }

    @Nullable
    abstract T getAdapter();
}
