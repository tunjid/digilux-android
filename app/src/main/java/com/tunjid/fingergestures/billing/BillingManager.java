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
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
    // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;

    private static final String TAG = "BillingManager";

    /**
     * A reference to BillingClient
     **/
    private BillingClient billingClient;

    /**
     * True if billing service is connected now.
     */
    private boolean isServiceConnected;

    private final BillingUpdatesListener billingUpdatesListener;

    private final Activity activity;

    private final List<Purchase> purchases = new ArrayList<>();

    private int billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "CONSTRUCT_YOUR_KEY_AND_PLACE_IT_HERE";

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();
        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(Activity activity, final BillingUpdatesListener updatesListener) {
        Log.d(TAG, "Creating Billing client.");
        this.activity = activity;
        billingUpdatesListener = updatesListener;
        billingClient = BillingClient.newBuilder(this.activity).setListener(this).build();

        Log.d(TAG, "Starting setup.");

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection(() -> {
            // Notifying the listener that billing client is ready
            billingUpdatesListener.onBillingClientSetupFinished();
            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(TAG, "Setup successful. Querying inventory.");
            queryPurchases();
        });
    }

    private void startServiceConnection(final Runnable executeOnSuccess) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
                Log.d(TAG, "Setup finished. Response code: " + billingResponseCode);

                if (billingResponseCode == BillingResponse.OK) {
                    isServiceConnected = true;
                    if (executeOnSuccess != null) executeOnSuccess.run();
                }
                billingClientResponseCode = billingResponseCode;
            }

            @Override
            public void onBillingServiceDisconnected() {
                isServiceConnected = false;
            }
        });
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
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
    public void initiatePurchaseFlow(final String skuId, final @SkuType String billingType) {
        executeServiceRequest(getPurchaseRequest(skuId, billingType));
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        executeServiceRequest(getPurchasesRunnable());
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

    public void querySkuDetailsAsync(@SkuType final String itemType, final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {
        // Creating a runnable from the request to use it inside our connection retry policy below
        Runnable queryRequest = () -> {
            // Query the purchase async
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(itemType);
            billingClient.querySkuDetailsAsync(params.build(), listener::onSkuDetailsResponse);
        };

        executeServiceRequest(queryRequest);
    }

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * clien connection response was not received yet.
     */
    public int getBillingClientResponseCode() {
        return billingClientResponseCode;
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

    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) runnable.run();
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
        else startServiceConnection(runnable);
    }

    @NonNull
    private Runnable getPurchaseRequest(String skuId, @SkuType String billingType) {
        return () -> {
            Log.d(TAG, "Launching in-app purchase flow. ");
            BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .build();
            billingClient.launchBillingFlow(activity, purchaseParams);
        };
    }

    @NonNull
    private Runnable getPurchasesRunnable() {
        return () -> {
            long time = System.currentTimeMillis();
            PurchasesResult purchasesResult = billingClient.queryPurchases(SkuType.INAPP);
            Log.i(TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                    + "ms");

             if (purchasesResult.getResponseCode() == BillingResponse.OK) {
                Log.i(TAG, "Skipped subscription purchases query since they are not supported");
            }
            else {
                Log.w(TAG, "queryPurchases() got an error response code: "
                        + purchasesResult.getResponseCode());
            }
            onQueryPurchasesFinished(purchasesResult);
        };
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (BASE_64_ENCODED_PUBLIC_KEY.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please update your app's public key at: "
                    + "BASE_64_ENCODED_PUBLIC_KEY");
        }

        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        }
        catch (IOException e) {
            Log.e(TAG, "Got an exception trying to validate a purchase: " + e);
            return false;
        }
    }
}

