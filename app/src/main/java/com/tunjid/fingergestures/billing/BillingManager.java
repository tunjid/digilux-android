/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tunjid.fingergestures.billing;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.tunjid.fingergestures.App;

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

    public BillingManager() {
        billingClient = BillingClient.newBuilder(App.getInstance()).setListener(PurchasesManager.getInstance()).build();
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
    public void consumeAsync(final String purchaseToken) {
        disposables.add(checkClient().subscribe(() -> billingClient.consumeAsync(purchaseToken, (a, b) -> {}), errorHandler));
    }

    private Completable checkClient() {return Completable.create(new BillingExecutor()).timeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS);}

    /**
     * Clear the resources
     */
    public void destroy() {
        if (billingClient != null) billingClient.endConnection();
        billingClient = null;
        disposables.dispose();
    }

    private class BillingExecutor implements CompletableOnSubscribe {

        private BillingExecutor() {}

        @Override
        public void subscribe(final CompletableEmitter emitter) throws Exception {
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

