package com.tunjid.fingergestures.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.viewholders.DiscreteItemViewHolder;

import androidx.annotation.NonNull;


public class DiscreteBrightnessAdapter extends DiffAdapter<DiscreteItemViewHolder, DiscreteBrightnessAdapter.BrightnessValueClickListener> {

    public DiscreteBrightnessAdapter(BrightnessValueClickListener listener) {
        super(BrightnessGestureConsumer.getInstance()::getDiscreteBrightnessValues, listener);
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public DiscreteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_chip, parent, false);
        return new DiscreteItemViewHolder(itemView, adapterListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscreteItemViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    public interface BrightnessValueClickListener extends InteractiveAdapter.AdapterListener {
        void onDiscreteBrightnessClicked(String discreteValue);
    }
}
