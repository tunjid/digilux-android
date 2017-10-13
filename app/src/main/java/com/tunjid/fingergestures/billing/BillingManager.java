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
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {

    //private static final int BILLING_MANAGER_NOT_INITIALIZED = -1;
    private static final String TAG = "BillingManager";
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg+iKb4z5KHff9rF1lsiWlp1K5Q6303Uo7eBFOOT1POH9XNz7uwEQebzCdqNZdzQKx94FTgdQhS5LY4NyacgzrvO7Nj1ceqTdsecnvncjFLrQOmmRYVoVI6hUsL8/dL1JvVSHq0xRLrFQxmO3p8FHQaIEf3qj2Q0L5LD01pNBxaPPKaaFasFbIQiWwA4mCchc7vnQdcRlVU00yCLxjux4yQejt1nKpHVUcrkuQcoDBWr31oMQiwX7OOTO6wLuKBw69Wd2AlnR3ynmRYkqCQOsRDajNWDAlWm8Z2VkgDX7pUFy8y8DQXqIXXYygLa+Mam33Z1/18OpQ51TghKRS2cC9QIDAQAB";

    //private int billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;
    private boolean isServiceConnected;

    private BillingClient billingClient;
    private final Activity activity;
    private final List<Purchase> purchases = new ArrayList<>();
    private final Consumer<Throwable> errorHandler = throwable -> {};
    private final BillingUpdatesListener billingUpdatesListener;

    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();

        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(Activity activity, final BillingUpdatesListener updatesListener) {
        this.activity = activity;
        billingUpdatesListener = updatesListener;
        billingClient = BillingClient.newBuilder(this.activity).setListener(this).build();

        checkClient().subscribe(() -> {
            billingUpdatesListener.onBillingClientSetupFinished();
            queryPurchases();
        }, errorHandler);
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
        if (purchases == null) return;
        if (resultCode == BillingResponse.OK) {
            for (Purchase purchase : purchases) handlePurchase(purchase);
            billingUpdatesListener.onPurchasesUpdated(this.purchases);
        }
        else if (resultCode == BillingResponse.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        }
        else {
            Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: " + resultCode);
        }
    }

    /**
     * Start a purchase flow
     */
    public Single<Integer> initiatePurchaseFlow(final String skuId, final @SkuType String billingType) {
        return checkClient().andThen(Single.fromCallable(() -> {
            Log.d(TAG, "Launching in-app purchase flow. ");
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .build();
            return billingClient.launchBillingFlow(activity, purchaseParams);
        }));
    }

    private void queryPurchases() {
        checkClient().subscribe(() -> {
            PurchasesResult purchasesResult = billingClient.queryPurchases(SkuType.INAPP);
            onQueryPurchasesFinished(purchasesResult);
        }, errorHandler);
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     *
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");
            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);
        purchases.add(purchase);
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private void onQueryPurchasesFinished(PurchasesResult result) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (billingClient == null || result.getResponseCode() != BillingResponse.OK) {
            Log.w(TAG, "Billing client was null or result code (" + result.getResponseCode()
                    + ") was bad - quitting");
            return;
        }

        Log.d(TAG, "Query inventory was successful.");

        // Update the UI and purchases inventory with new list of purchases
        purchases.clear();
        onPurchasesUpdated(BillingResponse.OK, result.getPurchasesList());
    }

    @SuppressWarnings("unused")
    public void consumeAsync(final String purchaseToken) {
        checkClient().subscribe(() -> billingClient.consumeAsync(purchaseToken, (a, b) -> {}), errorHandler);
    }

    private Completable checkClient() {return Completable.create(new BillingExecutor());}

    private boolean verifyValidSignature(String signedData, String signature) {
        try {return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);}
        catch (IOException e) {Log.e(TAG, "Got an exception trying to validate a purchase: " + e);}

        return false;
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying the manager.");

        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
            billingClient = null;
        }
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
                    Log.d(TAG, "Setup finished. Response code: " + billingResponseCode);

                    isServiceConnected = billingResponseCode == BillingResponse.OK;

                    if (isServiceConnected) emitter.onComplete();
                    else emitter.onError(new Exception("Inititalization Exception"));

//                    billingClientResponseCode = billingResponseCode;
                }

                @Override
                public void onBillingServiceDisconnected() {
                    isServiceConnected = false;
                }
            });
        }
    }
}

