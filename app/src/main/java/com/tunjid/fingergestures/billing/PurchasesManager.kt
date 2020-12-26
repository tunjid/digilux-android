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
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.jakewharton.rx.replayingShare
import com.tunjid.fingergestures.*
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.Flowables
import java.util.concurrent.TimeUnit


class PurchasesManager private constructor() : PurchasesUpdatedListener {

    enum class Sku(val id: String) {
        Premium(id = "premium"),
        AdFree(id = "ad.free");
    }

    private object SkuKey : ListPreference by object : ListPreference {
        override val preferenceName get() = "purchases"
    }

    data class State(
        val numTrials: Int,
        val trialStatus: TrialStatus,
        val hasLockedContent: Boolean,
        val ownedSkus: List<Sku>,
        val isPremium: Boolean,
    ) {
        private val isTrialRunning get() = trialStatus is TrialStatus.Trial

        val isOnTrial get() = trialStatus is TrialStatus.Trial

        val notAdFree: Boolean
            get() = when {
                !hasLockedContent -> false
                isTrialRunning -> false
                else -> !ownedSkus.contains(Sku.AdFree)
            }

        val hasAds get() = if (!hasLockedContent) false else !isPremium && notAdFree

        val trialPeriodText: String
            get() = App.transformApp({ app ->
                if (isTrialRunning) app.getString(R.string.trial_running)
                else app.getString(R.string.trial_text, when (trialPeriod(trialStatus.numTrials)) {
                    FIRST_TRIAL_PERIOD -> "10m"
                    SECOND_TRIAL_PERIOD -> "60s"
                    else -> "10s"
                })
            }, App.EMPTY)
    }

    val lockedContentPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = "has locked content",
        default = false,
        onSet = {
            App.withApp { app -> app.broadcast(Intent(ACTION_LOCKED_CONTENT_CHANGED)) }
        }
    )
    private val setManager: SetManager<SkuKey, Sku> = SetManager(
        keys = listOf(SkuKey),
        sorter = compareBy(Sku::ordinal),
        addFilter = Sku.values().map(Sku::id)::contains,
        stringMapper = { kind -> Sku.values().find { it.id == kind } },
        objectMapper = Sku::id
    )

    private val trigger = BehaviorProcessor.createDefault(false)

    private val trialStatus: Flowable<TrialStatus> = trigger
        .distinctUntilChanged()
        .scan(false to 0) { pair, isTrial -> pair.copy(first = isTrial, second = if (isTrial) pair.second + 1 else pair.second) }
        .concatMap { (trialRunning, numTrials) ->
            val trialPeriod = trialPeriod(numTrials)
            if (trialRunning) Flowable.intervalRange(0, trialPeriod.toLong(), 0, 1, TimeUnit.SECONDS)
                .map<TrialStatus> { elapsed -> TrialStatus.Trial(numTrials = numTrials, countDown = trialPeriod - elapsed) }
                .doFinally { trigger.onNext(false) }
            else Flowable.just(TrialStatus.Normal(numTrials = numTrials))
        }
        .replayingShare()


    val state: Flowable<State> = Flowables.combineLatest(
        lockedContentPreference.monitor,
        setManager.itemsFlowable(SkuKey),
        trialStatus
    ) { lockedContent, ownedSkus, trialStatus ->
        State(
            numTrials = trialStatus.numTrials,
            isPremium = if (!lockedContent) true else ownedSkus.contains(Sku.Premium),
            trialStatus = trialStatus,
            hasLockedContent = lockedContent,
            ownedSkus = ownedSkus
        )
    }
        .replayingShare()

    //        if (BuildConfig.DEV) return false;
    val isNotPremium: Boolean
        get() = when {
            !lockedContentPreference.value -> false
            isTrialRunning -> false
            else -> !setManager.getItems(SkuKey).contains(Sku.Premium)
        }

    val isPremium: Boolean
        get() = when {
            !lockedContentPreference.value -> true
            else -> !isNotPremium
        }

    //        if (BuildConfig.DEV) return true;
    val isPremiumNotTrial: Boolean
        get() = when {
            !lockedContentPreference.value -> true
            else -> setManager.getItems(SkuKey).contains(Sku.Premium)
        }

    val isTrialRunning: Boolean
        get() = trigger.value == true

    init {
        // Keep the Flowable alive
        trialStatus.subscribe()
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
        if (purchases == null) return
        if (responseCode != BillingClient.BillingResponse.OK) return

        purchases.filter(::filterPurchases)
            .map(Purchase::getSku)
            .filter(Sku.values().map(Sku::id)::contains)
            .forEach { setManager.addToSet(SkuKey, it) }
    }

    fun startTrial() = trigger.onNext(true)

    internal fun onPurchasesQueried(responseCode: Int, purchases: List<Purchase>?) {
        Sku.values().forEach { setManager.removeFromSet(SkuKey, it.id) }
        onPurchasesUpdated(responseCode, purchases)
    }

    internal fun clearPurchases() {
        Sku.values().forEach { setManager.removeFromSet(SkuKey, it.id) }
    }

    // App is open source, do a psuedo check.
    private fun filterPurchases(purchase: Purchase): Boolean {
        //        String json = purchase.getOriginalJson();
        //        String signature = purchase.getSignature();

        return !TextUtils.isEmpty(purchase.originalJson)
    }

    companion object {
        private const val FIRST_TRIAL_PERIOD = 60 * 10
        private const val SECOND_TRIAL_PERIOD = 60
        private const val FINAL_TRIAL_PERIOD = 10

        const val ACTION_LOCKED_CONTENT_CHANGED = "com.tunjid.fingergestures.action.lockedContentChanged"

        var instance = PurchasesManager()

        private fun trialPeriod(numTrials: Int) = when (numTrials) {
            0 -> FIRST_TRIAL_PERIOD
            1 -> SECOND_TRIAL_PERIOD
            else -> FINAL_TRIAL_PERIOD
        }
    }
}

sealed class TrialStatus(open val numTrials: Int) {
    data class Trial(override val numTrials: Int, val countDown: Long) : TrialStatus(numTrials)
    data class Normal(override val numTrials: Int) : TrialStatus(numTrials)
}