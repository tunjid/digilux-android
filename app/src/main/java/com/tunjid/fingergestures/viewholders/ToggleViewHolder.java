package com.tunjid.fingergestures.viewholders;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToggleViewHolder extends HomeViewHolder {

    public ToggleViewHolder(View itemView,
                            @StringRes int titleRes,
                            Supplier<Boolean> supplier,
                            Consumer<Boolean> consumer) {
        super(itemView);
        itemView.<TextView>findViewById(R.id.title).setText(titleRes);
        Switch toggle = itemView.findViewById(R.id.toggle);
        toggle.setChecked(supplier.get());
        toggle.setOnCheckedChangeListener((view, isChecked) -> consumer.accept(isChecked));
    }
}
