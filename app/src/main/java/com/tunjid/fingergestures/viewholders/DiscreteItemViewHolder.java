package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.DiscreteBrightnessAdapter;


public class DiscreteItemViewHolder extends InteractiveViewHolder<DiscreteBrightnessAdapter.BrightnessValueClickListener> {

    private String discreteValue;
    private TextView textView;

    public DiscreteItemViewHolder(View itemView, DiscreteBrightnessAdapter.BrightnessValueClickListener clickListener) {
        super(itemView, clickListener);
        textView = (TextView) itemView;

        itemView.setOnClickListener(view -> adapterListener.onDiscreteBrightnessClicked(discreteValue));
    }

    public void bind(String discreteValue) {
        this.discreteValue = discreteValue;
        Context context = itemView.getContext();
        textView.setText(context.getString(R.string.discrete_brightness_text, discreteValue));
    }
}
