package com.tunjid.fingergestures;

import com.android.billingclient.api.Purchase;
import com.tunjid.fingergestures.billing.BillingManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PurchasesManager implements BillingManager.BillingUpdatesListener {

    private static final String PURCHASES = "purchases";
    private static final String AD_FREE_SKU = "";
    private static final String PREMIUM_SKU = "";

    private static final Set<String> EMPTY = new HashSet<>();

    private static PurchasesManager instance;
    private final App app;

    public static PurchasesManager getInstance() {
        if (instance == null) instance = new PurchasesManager();
        return instance;
    }

    private PurchasesManager() {
        app = App.getInstance();
    }

    public boolean isNotPremium() {
        return !app.getPreferences().getStringSet(PURCHASES, EMPTY).contains(PREMIUM_SKU);
    }

    public boolean hasAds() {
        return !app.getPreferences().getStringSet(PURCHASES, EMPTY).contains(AD_FREE_SKU) || isNotPremium();
    }

    @Override
    public void onBillingClientSetupFinished() {

    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {
        Set<String> skus = app.getPreferences().getStringSet(PURCHASES, EMPTY);
        for (Purchase purchase : purchases) skus.add(purchase.getSku());
        app.getPreferences().edit().putStringSet(PURCHASES, skus).apply();
    }
}
