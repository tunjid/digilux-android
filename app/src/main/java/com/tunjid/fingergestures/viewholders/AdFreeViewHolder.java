package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import androidx.annotation.StringRes;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesVerifier;

public class AdFreeViewHolder extends AppViewHolder {

    private final PurchasesVerifier purchasesManager;
    private final TextView title;

    public AdFreeViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
        purchasesManager = PurchasesVerifier.getInstance();
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
                                ? PurchasesVerifier.AD_FREE_SKU
                                : PurchasesVerifier.PREMIUM_SKU)
                )
                .show();
    }

    private void showRemainingPurchaseOption() {

        boolean notPremium = purchasesManager.isNotPremium();

        Runnable action = notPremium
                ? () -> adapterListener.purchase(PurchasesVerifier.PREMIUM_SKU)
                : () -> adapterListener.purchase(PurchasesVerifier.AD_FREE_SKU);

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
