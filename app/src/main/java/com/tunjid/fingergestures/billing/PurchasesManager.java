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

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;

import static com.tunjid.fingergestures.App.transformApp;
import static com.tunjid.fingergestures.App.withApp;


public class PurchasesManager implements PurchasesUpdatedListener {

    private static final int FIRST_TRIAL_PERIOD = 60 * 10;
    private static final int SECOND_TRIAL_PERIOD = 60;
    private static final int FINAL_TRIAL_PERIOD = 10;


    public static final String AD_FREE_SKU = "ad.free";
    public static final String PREMIUM_SKU = "premium";
    public static final String ACTION_LOCKED_CONTENT_CHANGED = "com.tunjid.fingergestures.action.lockedContentChanged";
    private static final String PURCHASES = "purchases";
    private static final String HAS_LOCKED_CONTENT = "has locked content";
    private static final Set<String> EMPTY = new HashSet<>();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({AD_FREE_SKU, PREMIUM_SKU})
    public @interface SKU {}

    private static PurchasesManager instance;

    private int numTrials;
    private boolean isTrial;

    private Flowable<Long> trialFlowable;

    public static PurchasesManager getInstance() {
        if (instance == null) instance = new PurchasesManager();
        return instance;
    }

    private PurchasesManager() { }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (purchases == null) return;
        if (responseCode != BillingClient.BillingResponse.OK) return;

        SharedPreferences preferences = transformApp(App::getPreferences);
        if (preferences == null) return;

        Set<String> skus = new HashSet<>(preferences.getStringSet(PURCHASES, EMPTY));
        purchases.stream().filter(this::filterPurchases).map(Purchase::getSku).forEach(skus::add);

        preferences.edit().putStringSet(PURCHASES, skus).apply();
    }

    public void setHasLockedContent(boolean hasLockedContent) {
        withApp(app -> {
            app.getPreferences().edit().putBoolean(HAS_LOCKED_CONTENT, hasLockedContent).apply();
            app.broadcast(new Intent(ACTION_LOCKED_CONTENT_CHANGED));
        });
    }

    public boolean hasLockedContent() {
        return transformApp(app -> app.getPreferences().getBoolean(HAS_LOCKED_CONTENT, false), false);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isNotPremium() {
        if (!hasLockedContent()) return false;
        if (isTrial) return false;
//        if (BuildConfig.DEV) return false;
        return !transformApp(app -> getPurchaseSet(app).contains(PREMIUM_SKU), false);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean hasNotGoneAdFree() {
        if (!hasLockedContent()) return false;
        if (isTrial) return false;
        return !transformApp(app -> getPurchaseSet(app).contains(AD_FREE_SKU), false);
    }

    public boolean isPremium() {
        if (!hasLockedContent()) return true;
        return !isNotPremium();
    }

    public boolean isPremiumNotTrial() {
        if (!hasLockedContent()) return true;
//        if (BuildConfig.DEV) return true;
        return transformApp(app -> getPurchaseSet(app).contains(PREMIUM_SKU), false);
    }

    public boolean hasAds() {
        if (!hasLockedContent()) return false;
        return isNotPremium() && hasNotGoneAdFree();
    }

    @Nullable
    public Flowable<Long> getTrialFlowable() {
        return trialFlowable;
    }

    public void startTrial() {
        if (trialFlowable != null) return;
        int trialPeriod = getTrialPeriod();

        ConnectableFlowable<Long> actual = Flowable.intervalRange(0, trialPeriod, 0, 1, TimeUnit.SECONDS)
                .map(elapsed -> trialPeriod - elapsed)
                .doFinally(() -> {
                    isTrial = false;
                    trialFlowable = null;
                }).publish();

        actual.connect();

        trialFlowable = actual;
        isTrial = true;
        numTrials++;
    }

    public boolean isTrialRunning() {
        return trialFlowable != null;
    }

    public String getTrialPeriodText() {
        if (isTrialRunning())
            return transformApp(app -> app.getString(R.string.trial_running), App.EMPTY);

        int trialPeriod = getTrialPeriod();
        String periodText = trialPeriod == FIRST_TRIAL_PERIOD ? "10m" : trialPeriod == SECOND_TRIAL_PERIOD ? "60s" : "10s";

        return transformApp(app -> app.getString(R.string.trial_text, periodText), App.EMPTY);
    }

    void onPurchasesQueried(int responseCode, @Nullable List<Purchase> purchases) {
        withApp(app -> app.getPreferences().edit().remove(PURCHASES).apply());
        onPurchasesUpdated(responseCode, purchases);
    }

    void clearPurchases() {
        withApp(app -> app.getPreferences().edit().remove(PURCHASES).apply());
    }

    // App is open source, do a psuedo check.
    private boolean filterPurchases(Purchase purchase) {
//        String json = purchase.getOriginalJson();
//        String signature = purchase.getSignature();

        return !TextUtils.isEmpty(purchase.getOriginalJson());
    }

    private int getTrialPeriod() {
        return numTrials == 0 ? FIRST_TRIAL_PERIOD : numTrials == 1 ? SECOND_TRIAL_PERIOD : FINAL_TRIAL_PERIOD;
    }

    private Set<String> getPurchaseSet(App app) {
        return app.getPreferences().getStringSet(PURCHASES, EMPTY);
    }
}
