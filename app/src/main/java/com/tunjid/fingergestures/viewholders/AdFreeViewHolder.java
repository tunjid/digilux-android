package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

public class AdFreeViewHolder extends AppViewHolder {

    private final PurchasesManager purchasesManager;
    private final TextView title;

    public AdFreeViewHolder(View itemView, AppAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
        purchasesManager = PurchasesManager.getInstance();
        title = itemView.findViewById(R.id.title);
    }

    @Override
    public void bind() {
        super.bind();
        boolean hasAds = purchasesManager.hasAds();
        boolean notAdFree = purchasesManager.hasNotGoneAdFree();
        boolean notPremium = purchasesManager.isNotPremium();

        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(title, hasAds
                ? R.drawable.ic_attach_money_white_24dp
                : R.drawable.ic_check_white_24dp, 0, 0, 0);

        title.setText(hasAds
                ? R.string.go_ad_free_title
                : notPremium
                ? R.string.ad_free_no_ad_user
                : R.string.ad_free_premium_user);

        View.OnClickListener goAdFree = v -> {
            if (hasAds) showPurchaseOptions();
            else showRemainingPurchaseOption();
        };

        itemView.setOnClickListener(notAdFree || notPremium ? goAdFree : null);
    }

    private void showPurchaseOptions() {
        Context context = itemView.getContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.purchase_options)
                .setItems(R.array.purchase_options, (dialog, index) ->
                        adapterListener.purchase(index == 0
                                ? PurchasesManager.AD_FREE_SKU
                                : PurchasesManager.PREMIUM_SKU)
                )
                .show();
    }

    private void showRemainingPurchaseOption() {

        boolean notPremium = purchasesManager.isNotPremium();

        Runnable action = notPremium
                ? () -> adapterListener.purchase(PurchasesManager.PREMIUM_SKU)
                : () -> adapterListener.purchase(PurchasesManager.AD_FREE_SKU);

        @StringRes int title = notPremium
                ? R.string.go_premium_title
                : R.string.premium_generosity_title;

        @StringRes int description = notPremium
                ? R.string.go_premium_confirmation
                : R.string.premium_generosity_confirmation;

        Context context = itemView.getContext();
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(description)
                .setPositiveButton(R.string.continue_text, (dialog, which) -> action.run())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
