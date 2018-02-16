package com.tunjid.fingergestures.viewholders;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;


public class ActionViewHolder extends BaseViewHolder<ActionAdapter.ActionClickListener> {

    private int stringRes;
    private TextView textView;

    public ActionViewHolder(View itemView, ActionAdapter.ActionClickListener clickListener) {
        super(itemView, clickListener);
        textView = itemView.findViewById(R.id.title);

        itemView.setOnClickListener(view -> adapterListener.onActionClicked(stringRes));
    }

    public void bind(@DrawableRes int drawableRes, @StringRes int stringRes) {
        this.stringRes = stringRes;

        textView.setText(stringRes);
        textView.setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0);
    }
}
