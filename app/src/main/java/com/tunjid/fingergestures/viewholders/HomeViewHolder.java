package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

public class HomeViewHolder extends BaseViewHolder<HomeAdapter.HomeAdapterListener> {

    public HomeViewHolder(View itemView) {
        super(itemView);
    }

    HomeViewHolder(View itemView, HomeAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
    }

    public void bind() {}

    void goPremium(@StringRes int description) {
        Context context = itemView.getContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.go_premium_title)
                .setMessage(context.getString(R.string.go_premium_body, context.getString(description)))
                .setPositiveButton(R.string.continue_text, (dialog, which) -> adapterListener.purchase(PurchasesManager.PREMIUM_SKU))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
