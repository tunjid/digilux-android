package com.tunjid.fingergestures.adapters;

import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.List;


public class ActionAdapter extends BaseRecyclerViewAdapter<ActionViewHolder, ActionAdapter.ActionClickListener> {

    private final boolean isHorizontal;
    private final boolean showsText;
    private final List<String> actionStrings;

    public ActionAdapter(boolean isHorizontal, boolean showsText, List<String> actionStrings, ActionClickListener listener) {
        super(listener);
        this.isHorizontal = isHorizontal;
        this.showsText = showsText;
        this.actionStrings = actionStrings;
        setHasStableIds(true);
    }

    @Override
    public ActionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layoutRes = isHorizontal ? R.layout.viewholder_action_horizontal : R.layout.viewholder_action_vertical;
        return new ActionViewHolder(showsText, LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(ActionViewHolder holder, int position) {
        holder.bind(Integer.valueOf(actionStrings.get(position)));
    }

    @Override
    public int getItemCount() {
        return actionStrings.size();
    }

    @Override
    public long getItemId(int position) {
        return actionStrings.get(position).hashCode();
    }

    public interface ActionClickListener extends BaseRecyclerViewAdapter.AdapterListener {
        void onActionClicked(@GestureConsumer.GestureAction int actionRes);
    }
}