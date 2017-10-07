package com.tunjid.fingergestures.viewholders;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.tunjid.fingergestures.R;

import java.util.function.Consumer;

public class ToggleViewHolder extends HomeViewHolder {

    public ToggleViewHolder(View itemView,
                            @StringRes int titleRes,
                            Consumer<Boolean> consumer) {
        super(itemView);
        itemView.<TextView>findViewById(R.id.title).setText(titleRes);
        Switch toggle = itemView.findViewById(R.id.toggle);
        toggle.setOnCheckedChangeListener((view, isChecked) -> consumer.accept(isChecked));
    }
}
