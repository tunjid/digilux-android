/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import androidx.annotation.StringRes;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

public class AdFreeViewHolder extends AppViewHolder {

    private final PurchasesManager purchasesManager;
    private final TextView title;

    public AdFreeViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
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
