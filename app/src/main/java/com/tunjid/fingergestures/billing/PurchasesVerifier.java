package com.tunjid.fingergestures.billing;

import android.content.SharedPreferences;

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


public class PurchasesVerifier implements PurchasesUpdatedListener {

    private static final int FIRST_TRIAL_PERIOD = 60 * 10;
    private static final int SECOND_TRIAL_PERIOD = 60;
    private static final int FINAL_TRIAL_PERIOD = 10;


    public static final String AD_FREE_SKU = "ad.free";
    public static final String PREMIUM_SKU = "premium";
    private static final String PURCHASES = "purchases";
    private static final Set<String> EMPTY = new HashSet<>();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({AD_FREE_SKU, PREMIUM_SKU})
    public @interface SKU {}

    private static PurchasesVerifier instance;

    private int numTrials;
    private boolean isTrial;

    private Flowable<Long> trialFlowable;

    public static PurchasesVerifier getInstance() {
        if (instance == null) instance = new PurchasesVerifier();
        return instance;
    }

    private PurchasesVerifier() { }

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

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isNotPremium() {
        return false;
//        if (isTrial) return false;
//        if (BuildConfig.DEV) return false;
//        return !transformApp(app -> app.getPreferences().getStringSet(PURCHASES, EMPTY).contains(PREMIUM_SKU), false);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    public boolean hasNotGoneAdFree() {
        return false;
//        if (isTrial) return false;
//        return !transformApp(app -> app.getPreferences().getStringSet(PURCHASES, EMPTY).contains(AD_FREE_SKU), false);
    }

    public boolean isPremium() {
        return true;
//        return !isNotPremium();
    }

    public boolean isPremiumNotTrial() {
        return true;
//        if (BuildConfig.DEV) return true;
//        return transformApp(app -> app.getPreferences().getStringSet(PURCHASES, EMPTY).contains(PREMIUM_SKU), false);
    }

    public boolean hasAds() {
        return false;
//        return isNotPremium() && hasNotGoneAdFree();
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

    private boolean filterPurchases(Purchase purchase) {
        String json = purchase.getOriginalJson();
        String signature = purchase.getSignature();

        return isTrue(json, signature);
    }

    private int getTrialPeriod() {
        return numTrials == 0 ? FIRST_TRIAL_PERIOD : numTrials == 1 ? SECOND_TRIAL_PERIOD : FINAL_TRIAL_PERIOD;
    }

    private boolean isTrue(String json, String signature) {
        return true;
    }
}
