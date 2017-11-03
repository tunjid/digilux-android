package com.tunjid.fingergestures.viewholders;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SliderAdjusterViewHolder extends HomeViewHolder
        implements SeekBar.OnSeekBarChangeListener {

    private final TextView value;
    private final SeekBar seekBar;
    private final Consumer<Integer> consumer;
    private final Supplier<Integer> valueSupplier;
    private final Supplier<Boolean> enabledSupplier;
    private final Function<Integer, String> function;

    public SliderAdjusterViewHolder(View itemView,
                                    @StringRes int titleRes,
                                    Consumer<Integer> consumer,
                                    Supplier<Integer> valueSupplier,
                                    Supplier<Boolean> enabledSupplier,
                                    Function<Integer, String> function) {
        super(itemView);
        this.consumer = consumer;
        this.valueSupplier = valueSupplier;
        this.enabledSupplier = enabledSupplier;
        this.function = function;

        value = itemView.findViewById(R.id.value);

        itemView.<TextView>findViewById(R.id.title).setText(titleRes);

        seekBar = itemView.findViewById(R.id.seekbar);
        seekBar.setProgress(valueSupplier.get());
        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void bind() {
        super.bind();
        boolean enabled = enabledSupplier.get();
        value.setEnabled(enabled);
        seekBar.setEnabled(enabled);
        value.setText(function.apply(valueSupplier.get()));
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
