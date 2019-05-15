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

package com.tunjid.fingergestures.baseclasses;


import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity;
import com.tunjid.androidbootstrap.functions.Consumer;
import com.tunjid.androidbootstrap.view.animator.ViewHider;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.BillingManager;
import com.tunjid.fingergestures.billing.PurchasesManager;

import io.reactivex.disposables.CompositeDisposable;

import static com.google.android.material.snackbar.Snackbar.LENGTH_SHORT;
import static com.android.billingclient.api.BillingClient.BillingResponse.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponse.OK;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_UNAVAILABLE;

public abstract class FingerGestureActivity extends BaseActivity {

    protected ViewHider barHider;
    protected ViewHider fabHider;
    protected ViewGroup coordinator;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    private BillingManager billingManager;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        coordinator = findViewById(R.id.container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        billingManager = new BillingManager(getApplicationContext());
    }

    @Override
    protected void onStop() {
        if (billingManager != null) billingManager.destroy();
        billingManager = null;
        disposables.dispose();
        super.onStop();
    }

    public void showSnackbar(@StringRes int resource) {
        withSnackbar(snackbar -> {
            snackbar.setText(resource);
            snackbar.show();
        });
    }

    public void toggleToolbar(boolean visible) {
        if (visible) barHider.show();
        else barHider.hide();
    }

    public void purchase(@PurchasesManager.SKU String sku) {
        if (billingManager == null) showSnackbar(R.string.generic_error);
        else disposables.add(billingManager.initiatePurchaseFlow(this, sku)
                .subscribe(launchStatus -> {
                    switch (launchStatus) {
                        case OK:
                            break;
                        case SERVICE_UNAVAILABLE:
                        case SERVICE_DISCONNECTED:
                            showSnackbar(R.string.billing_not_connected);
                            break;
                        case ITEM_ALREADY_OWNED:
                            showSnackbar(R.string.billing_you_own_this);
                            break;
                        default:
                            showSnackbar(R.string.generic_error);
                            break;
                    }
                }, throwable -> showSnackbar(R.string.generic_error)));
    }

    protected void withSnackbar(Consumer<Snackbar> consumer) {
        Snackbar snackbar = Snackbar.make(coordinator, R.string.app_name, LENGTH_SHORT);
        snackbar.getView().setOnApplyWindowInsetsListener((view, insets) -> insets);
        consumer.accept(snackbar);
    }
}
