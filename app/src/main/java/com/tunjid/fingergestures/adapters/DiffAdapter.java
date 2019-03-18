package com.tunjid.fingergestures.adapters;

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;

import java.util.List;

public abstract class DiffAdapter<V extends InteractiveViewHolder<T>, T extends InteractiveAdapter.AdapterListener, S>
        extends InteractiveAdapter<V, T> {

    final List<S> list;

    DiffAdapter(List<S> list, T listener) {
        super(listener);
        this.list = list;
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

}
