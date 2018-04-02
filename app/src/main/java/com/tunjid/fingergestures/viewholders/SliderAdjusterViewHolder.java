package com.tunjid.fingergestures.viewholders;

import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SliderAdjusterViewHolder extends AppViewHolder
        implements SeekBar.OnSeekBarChangeListener {

    private final TextView value;
    private final SeekBar seekBar;
    private final Consumer<Integer> consumer;
    private final Supplier<Integer> valueSupplier;
    private final Supplier<Boolean> enabledSupplier;
    private final Function<Integer, String> function;

    public SliderAdjusterViewHolder(View itemView,
                                    @StringRes int titleRes,
                                    @StringRes int infoRes,
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

        TextView title = itemView.findViewById(R.id.title);
        title.setText(titleRes);

        seekBar = itemView.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);

        if (infoRes != 0) {
            title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_info_outline_white_24dp, 0);
            itemView.setOnClickListener(v -> new AlertDialog.Builder(itemView.getContext())
                    .setMessage(infoRes)
                    .show());
        }
    }

    public SliderAdjusterViewHolder(View itemView,
                                    @StringRes int titleRes,
                                    Consumer<Integer> consumer,
                                    Supplier<Integer> valueSupplier,
                                    Supplier<Boolean> enabledSupplier,
                                    Function<Integer, String> function) {
        this(itemView, titleRes, 0, consumer, valueSupplier, enabledSupplier, function);
    }

    @Override
    public void bind() {
        super.bind();
        boolean enabled = enabledSupplier.get();

        seekBar.setEnabled(enabled);
        seekBar.setProgress(valueSupplier.get());

        value.setEnabled(enabled);
        value.setText(function.apply(valueSupplier.get()));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int percentage, boolean fromUser) {
        if (!fromUser) return;
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
