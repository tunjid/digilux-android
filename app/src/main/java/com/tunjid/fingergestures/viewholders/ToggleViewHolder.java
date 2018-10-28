package com.tunjid.fingergestures.viewholders;

import androidx.annotation.StringRes;
import android.view.View;
import android.widget.Switch;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToggleViewHolder extends AppViewHolder {

    public ToggleViewHolder(View itemView,
                            @StringRes int titleRes,
                            Supplier<Boolean> supplier,
                            Consumer<Boolean> consumer) {
        super(itemView);
        Switch toggle = itemView.findViewById(R.id.toggle);
        toggle.setText(titleRes);
        toggle.setChecked(supplier.get());
        toggle.setOnCheckedChangeListener((view, isChecked) -> consumer.accept(isChecked));
    }
}
