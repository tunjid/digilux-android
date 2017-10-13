package com.tunjid.fingergestures.viewholders;

import android.support.v7.app.AlertDialog;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;

public class HomeViewHolder extends BaseViewHolder<HomeAdapter.HomeAdapterListener> {

    public HomeViewHolder(View itemView) {
        super(itemView);
    }

    public void bind() {}

    void goPremium() {
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.go_premium_title)
                .setMessage(R.string.go_premium_body)
                .setPositiveButton(R.string.yes, (dialog, which) -> adapterListener.goPremium())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
