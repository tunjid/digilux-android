package com.tunjid.fingergestures.adapters;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.view.recyclerview.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.List;
import java.util.function.Supplier;


public class ActionAdapter extends DiffAdapter<ActionViewHolder, ActionAdapter.ActionClickListener> {

    private final boolean isHorizontal;
    private final boolean showsText;

    public ActionAdapter(boolean isHorizontal, boolean showsText, Supplier<List<String>> listSupplier, ActionClickListener listener) {
        super(listSupplier, listener);
        this.isHorizontal = isHorizontal;
        this.showsText = showsText;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @LayoutRes int layoutRes = isHorizontal ? R.layout.viewholder_action_horizontal : R.layout.viewholder_action_vertical;
        return new ActionViewHolder(showsText, LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false), adapterListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        holder.bind(Integer.valueOf(list.get(position)));
    }

    public interface ActionClickListener extends BaseRecyclerViewAdapter.AdapterListener {
        void onActionClicked(@GestureConsumer.GestureAction int actionRes);
    }
}
