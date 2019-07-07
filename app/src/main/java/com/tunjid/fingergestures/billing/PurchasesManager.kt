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

package com.tunjid.fingergestures.billing

import android.content.Intent
import android.text.TextUtils
import androidx.annotation.StringDef
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import io.reactivex.Flowable
import java.util.*
import java.util.concurrent.TimeUnit


class PurchasesManager private constructor() : PurchasesUpdatedListener {

    private var numTrials: Int = 0
    private var isTrial: Boolean = false

    var trialFlowable: Flowable<Long>? = null
        private set

    //        if (BuildConfig.DEV) return false;
    val isNotPremium: Boolean
        get() {
            if (!hasLockedContent()) return false
            return if (isTrial) false else !App.transformApp({ app -> getPurchaseSet(app).contains(PREMIUM_SKU) }, false)
        }

    val isPremium: Boolean
        get() = if (!hasLockedContent()) true else !isNotPremium

    //        if (BuildConfig.DEV) return true;
    val isPremiumNotTrial: Boolean
        get() = if (!hasLockedContent()) true else App.transformApp({ app -> getPurchaseSet(app).contains(PREMIUM_SKU) }, false)

    val isTrialRunning: Boolean
        get() = trialFlowable != null

    val trialPeriodText: String
        get() {
            if (isTrialRunning)
                return App.transformApp({ app -> app.getString(R.string.trial_running) }, App.EMPTY)

            val trialPeriod = trialPeriod
            val periodText = if (trialPeriod == FIRST_TRIAL_PERIOD) "10m" else if (trialPeriod == SECOND_TRIAL_PERIOD) "60s" else "10s"

            return App.transformApp({ app -> app.getString(R.string.trial_text, periodText) }, App.EMPTY)
        }

    private val trialPeriod: Int
        get() = if (numTrials == 0) FIRST_TRIAL_PERIOD else if (numTrials == 1) SECOND_TRIAL_PERIOD else FINAL_TRIAL_PERIOD

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(AD_FREE_SKU, PREMIUM_SKU)
    annotation class SKU

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
        if (purchases == null) return
        if (responseCode != BillingClient.BillingResponse.OK) return

        val preferences = App.transformApp(({ it.preferences })) ?: return

        val skus = HashSet<String>(preferences.getStringSet(PURCHASES, EMPTY))
        purchases.filter(this::filterPurchases).map(Purchase::getSku).forEach(skus::add)

        preferences.edit().putStringSet(PURCHASES, skus).apply()
    }

    fun setHasLockedContent(hasLockedContent: Boolean) {
        App.withApp { app ->
            app.preferences.edit().putBoolean(HAS_LOCKED_CONTENT, hasLockedContent).apply()
            app.broadcast(Intent(ACTION_LOCKED_CONTENT_CHANGED))
        }
    }

    fun hasLockedContent(): Boolean {
        return App.transformApp({ app -> app.preferences.getBoolean(HAS_LOCKED_CONTENT, false) }, false)
    }

    fun hasNotGoneAdFree(): Boolean {
        if (!hasLockedContent()) return false
        return if (isTrial) false else !App.transformApp({ app -> getPurchaseSet(app)!!.contains(AD_FREE_SKU) }, false)
    }

    fun hasAds(): Boolean {
        return if (!hasLockedContent()) false else isNotPremium && hasNotGoneAdFree()
    }

    fun startTrial() {
        if (trialFlowable != null) return
        val trialPeriod = trialPeriod

        val actual = Flowable.intervalRange(0, trialPeriod.toLong(), 0, 1, TimeUnit.SECONDS)
                .map { elapsed -> trialPeriod - elapsed }
                .doFinally {
                    isTrial = false
                    trialFlowable = null
                }.publish()

        actual.connect()

        trialFlowable = actual
        isTrial = true
        numTrials++
    }

    internal fun onPurchasesQueried(responseCode: Int, purchases: List<Purchase>?) {
        App.withApp { app -> app.preferences.edit().remove(PURCHASES).apply() }
        onPurchasesUpdated(responseCode, purchases)
    }

    internal fun clearPurchases() {
        App.withApp { app -> app.preferences.edit().remove(PURCHASES).apply() }
    }

    // App is open source, do a psuedo check.
    private fun filterPurchases(purchase: Purchase): Boolean {
        //        String json = purchase.getOriginalJson();
        //        String signature = purchase.getSignature();

        return !TextUtils.isEmpty(purchase.originalJson)
    }

    private fun getPurchaseSet(app: App): Set<String> {
        return app.preferences.getStringSet(PURCHASES, EMPTY)
    }

    companion object {

        private const val FIRST_TRIAL_PERIOD = 60 * 10
        private const val SECOND_TRIAL_PERIOD = 60
        private const val FINAL_TRIAL_PERIOD = 10


        const val AD_FREE_SKU = "ad.free"
        const val PREMIUM_SKU = "premium"
        const val ACTION_LOCKED_CONTENT_CHANGED = "com.tunjid.fingergestures.action.lockedContentChanged"

        private const val PURCHASES = "purchases"
        private const val HAS_LOCKED_CONTENT = "has locked content"
        private val EMPTY = HashSet<String>()

        var instance = PurchasesManager()
    }
}
