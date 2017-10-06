package com.tunjid.fingergestures.viewholders;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;
import java.util.function.Function;

public class SliderAdjusterViewHolder extends HomeViewHolder
        implements SeekBar.OnSeekBarChangeListener {

    private final TextView value;
    private final Consumer<Integer> consumer;
    private final Function<Integer, String> function;

    public SliderAdjusterViewHolder(View itemView,
                                    @StringRes int titleRes,
                                    int initialPercentage,
                                    Consumer<Integer> consumer,
                                    Function<Integer, String> function) {
        super(itemView);
        this.consumer = consumer;
        this.function = function;

        value = itemView.findViewById(R.id.value);
        value.setText(function.apply(initialPercentage));

        itemView.<TextView>findViewById(R.id.title).setText(titleRes);

        SeekBar seekBar = itemView.findViewById(R.id.seekbar);
        seekBar.setProgress(initialPercentage);
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int percentage, boolean fromUser) {
        consumer.accept(percentage);
        value.setText(function.apply(percentage));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
