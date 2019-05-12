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
import com.tunjid.fingergestures.billing.PurchasesVerifier;

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
        withSnackbar(snackbar -> {
            snackbar.setText(resource);
            snackbar.show();
        });
    }

    public void toggleToolbar(boolean visible) {
        if (visible) barHider.show();
        else barHider.hide();
    }

    public void purchase(@PurchasesVerifier.SKU String sku) {
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
