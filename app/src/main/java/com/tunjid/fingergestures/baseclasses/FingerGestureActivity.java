package com.tunjid.fingergestures.baseclasses;


import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity;
import com.tunjid.androidbootstrap.core.view.ViewHider;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.BillingManager;
import com.tunjid.fingergestures.billing.PurchasesManager;

import io.reactivex.disposables.CompositeDisposable;

import static android.support.design.widget.Snackbar.LENGTH_SHORT;
import static com.android.billingclient.api.BillingClient.BillingResponse.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponse.OK;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_UNAVAILABLE;

public abstract class FingerGestureActivity extends BaseActivity {

    protected ViewHider fabHider;
    protected ViewHider barHider;
    protected FloatingActionButton fab;

    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    private BillingManager billingManager;

    @Override
    protected void onResume() {
        super.onResume();
        billingManager = new BillingManager();
    }

    @Override
    protected void onStop() {
        if (billingManager != null) billingManager.destroy();
        billingManager = null;
        disposables.dispose();
        super.onStop();
    }

    public void showSnackbar(@StringRes int resource) {
        ViewGroup root = findViewById(R.id.container);
        if (root == null) return;
        Snackbar.make(root, resource, LENGTH_SHORT).show();
    }

    public void toggleFab(boolean visible) {
        if (visible) fabHider.show();
        else fabHider.hide();
    }

    public void toggleToolbar(boolean visible) {
        if (visible) barHider.show();
        else barHider.hide();
    }

    public void purchase(@PurchasesManager.SKU String sku) {
        if (billingManager == null) showSnackbar(R.string.billing_generic_error);
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
                            showSnackbar(R.string.billing_generic_error);
                            break;
                    }
                }, throwable -> showSnackbar(R.string.billing_generic_error)));
    }
}
