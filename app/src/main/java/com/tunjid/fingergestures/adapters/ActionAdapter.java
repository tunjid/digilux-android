package com.tunjid.fingergestures.adapters;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.List;


public class ActionAdapter extends BaseRecyclerViewAdapter<ActionViewHolder, ActionAdapter.ActionClickListener> {

    private final List<Pair<Integer, Integer>> resources;

    public ActionAdapter(List<Pair<Integer, Integer>> resources, ActionClickListener listener) {
        super(listener);
        setHasStableIds(true);
        this.resources = resources;
    }

    @Override
    public ActionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ActionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_action, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(ActionViewHolder holder, int position) {
        Pair<Integer, Integer> pair = resources.get(position);
        holder.bind(pair.first, pair.second);
    }

    @Override
    public int getItemCount() {
        return resources.size();
    }

    @Override
    public long getItemId(int position) {
        return resources.get(position).hashCode();
    }

    public interface ActionClickListener extends BaseRecyclerViewAdapter.AdapterListener {
        void onActionClicked(int actionRes);
    }
}
