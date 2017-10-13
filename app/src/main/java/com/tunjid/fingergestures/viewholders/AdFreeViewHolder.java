package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

public class AdFreeViewHolder extends HomeViewHolder {

    private final PurchasesManager purchasesManager;
    private final TextView title;

    public AdFreeViewHolder(View itemView, HomeAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
        purchasesManager = PurchasesManager.getInstance();
        title = itemView.findViewById(R.id.title);
    }

    @Override
    public void bind() {
        super.bind();

        title.setText(purchasesManager.hasAds()
                ? R.string.ad_free_basic_user
                : !purchasesManager.isNotPremium()
                ? R.string.ad_free_premium_user
                : R.string.ad_free_no_ad_user);

        if (purchasesManager.hasNotGoneAdFree()) {
            itemView.setOnClickListener(v -> removeAds(purchasesManager.isNotPremium()
                            ? R.string.ad_free_basic_user_confirmation
                            : R.string.ad_free_premium_user_confirmation));
        }
    }

    void removeAds(@StringRes int description) {
        Context context = itemView.getContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.remove_ads)
                .setMessage(description)
                .setPositiveButton(R.string.yes, (dialog, which) -> adapterListener.purchase(PurchasesManager.AD_FREE_SKU))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
