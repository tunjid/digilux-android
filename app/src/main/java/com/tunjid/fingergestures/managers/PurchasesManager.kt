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

package com.tunjid.fingergestures.managers

import android.content.Context
import android.text.TextUtils
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.jakewharton.rx.replayingShare
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.AppDisposable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.rxkotlin.Flowables
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private object Purchases : SetPreference {
    override val preferenceName get() = "purchases"
}

@Singleton
class PurchasesManager @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    appDisposable: AppDisposable,
) : PurchasesUpdatedListener {

    enum class Sku(val id: String) {
        Premium(id = "premium"),
        AdFree(id = "ad.free");
    }

    data class State(
        val numTrials: Int = 0,
        val trialStatus: TrialStatus = TrialStatus.Normal(numTrials = 0),
        val hasLockedContent: Boolean = false,
        val ownedSkus: List<Sku> = listOf(),
        val isPremium: Boolean = true,
        val trialPeriodText: String = ""
    ) {
        val isOnTrial get() = trialStatus is TrialStatus.Trial

        val notAdFree: Boolean
            get() = when {
                !hasLockedContent -> false
                isOnTrial -> false
                else -> !ownedSkus.contains(Sku.AdFree)
            }

        val hasAds get() = if (!hasLockedContent) false else !isPremium && notAdFree

        val isPremiumNotTrial: Boolean get() = if (hasLockedContent) false else isPremium
    }

    val lockedContentPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "has locked content",
        default = false
    )
    private val setManager = SetManager(
        reactivePreferences = reactivePreferences,
        keys = listOf(Purchases),
        sorter = compareBy(Sku::ordinal),
        addFilter = Sku.values().map(Sku::id)::contains,
        stringMapper = { kind -> Sku.values().find { it.id == kind } },
        objectMapper = Sku::id
    )
    private val editor: SetPreferenceEditor<Sku> = setManager.editorFor(Purchases)

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
        setManager.itemsFor(Purchases),
        trialStatus,
        trigger
    ) { lockedContent, ownedSkus, trialStatus, trialRunning ->
        State(
            numTrials = trialStatus.numTrials,
            isPremium = when {
                !lockedContent -> true
                trialRunning -> false
                else -> ownedSkus.contains(Sku.Premium)
            },
            trialStatus = trialStatus,
            hasLockedContent = lockedContent,
            ownedSkus = ownedSkus,
            trialPeriodText = if (trialRunning) context.getString(R.string.trial_running)
            else context.getString(R.string.trial_text, when (trialPeriod(trialStatus.numTrials)) {
                FIRST_TRIAL_PERIOD -> "10m"
                SECOND_TRIAL_PERIOD -> "60s"
                else -> "10s"
            })
        )
    }
        .replayingShare()

    val currentState by state.asProperty(State(), appDisposable::add)

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (purchases == null) return
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return

        purchases.filter(::filterPurchases)
            .map(Purchase::getSku)
            .mapNotNull { id -> Sku.values().firstOrNull { it.id == id } }
            .forEach(editor::plus)
    }

    fun startTrial() = trigger.onNext(true)

    internal fun onPurchasesQueried(result: Purchase.PurchasesResult) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        Sku.values().forEach(editor::minus)
        onPurchasesUpdated(result.billingResult, result.purchasesList)
    }

//    internal fun clearPurchases() = Sku.values().forEach(editor::minus)

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