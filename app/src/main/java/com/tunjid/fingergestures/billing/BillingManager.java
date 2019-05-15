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
package com.tunjid.fingergestures.billing;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager {

    private static final int CONNECTION_TIMEOUT = 5;
    private boolean isServiceConnected;

    private BillingClient billingClient;
    private CompositeDisposable disposables = new CompositeDisposable();
    private final Consumer<Throwable> errorHandler = throwable -> {};

    public BillingManager(Context context) {
        billingClient = BillingClient.newBuilder(context).setListener(PurchasesManager.getInstance()).build();
        disposables.add(checkClient().subscribe(this::queryPurchases, errorHandler));
    }

    /**
     * Start a purchase flow
     */
    public Single<Integer> initiatePurchaseFlow(Activity activity, final String skuId) {
        return checkClient().andThen(Single.fromCallable(() -> {
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(INAPP)
                    .build();
            return billingClient.launchBillingFlow(activity, purchaseParams);
        }));
    }

    private void queryPurchases() {
        disposables.add(checkClient().subscribe(() -> {
            PurchasesResult result = billingClient.queryPurchases(SkuType.INAPP);
            if (billingClient == null || result.getResponseCode() != BillingResponse.OK) return;

            PurchasesManager purchasesManager = PurchasesManager.getInstance();
            purchasesManager.onPurchasesQueried(result.getResponseCode(), result.getPurchasesList());
        }, errorHandler));
    }

    @SuppressWarnings("unused")
    private void consume(final String purchaseToken) {
        disposables.add(checkClient().subscribe(() -> billingClient.consumeAsync(purchaseToken, (a, b) -> {}), errorHandler));
    }

    @SuppressWarnings("unused")
    private void consumeAll() {
        disposables.add(checkClient().subscribe(() -> {
            PurchasesManager.getInstance().clearPurchases();
            PurchasesResult result = billingClient.queryPurchases(SkuType.INAPP);
            if (billingClient == null || result.getResponseCode() != BillingResponse.OK) return;
            for (Purchase item : result.getPurchasesList()) consume(item.getPurchaseToken());
        }, errorHandler));
    }

    private Completable checkClient() {return Completable.create(new BillingExecutor()).timeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS);}

    /**
     * Clear the resources
     */
    public void destroy() {
        try {if (billingClient != null) billingClient.endConnection();}
        catch (Exception e) {e.printStackTrace();}
        billingClient = null;
        disposables.dispose();
    }

    private class BillingExecutor implements CompletableOnSubscribe {

        private BillingExecutor() {}

        @Override
        public void subscribe(final CompletableEmitter emitter) {
            if (isServiceConnected) {
                emitter.onComplete();
                return;
            }

            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
                    isServiceConnected = billingResponseCode == BillingResponse.OK;

                    if (isServiceConnected) emitter.onComplete();
                    else emitter.onError(new Exception("Inititalization Exception"));
                }

                @Override
                public void onBillingServiceDisconnected() {
                    isServiceConnected = false;
                }
            });
        }
    }
}

