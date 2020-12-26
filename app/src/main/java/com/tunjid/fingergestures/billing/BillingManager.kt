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

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.CompletableOnSubscribe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
class BillingManager(context: Context) {
    private var isServiceConnected: Boolean = false

    private var billingClient: BillingClient? = null
    private val disposables = CompositeDisposable()
    private val errorHandler = { it: Throwable -> it.printStackTrace() }

    init {
        billingClient = BillingClient.newBuilder(context).setListener(PurchasesManager.instance).build()
        disposables.add(checkClient().subscribe({ this.queryPurchases() }, errorHandler))
    }

    /**
     * Start a purchase flow
     */
    fun initiatePurchaseFlow(activity: Activity, sku: PurchasesManager.Sku): Single<Int> {
        return checkClient().andThen(Single.fromCallable {
            val purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(sku.id)
                    .setType(INAPP)
                    .build()
            billingClient!!.launchBillingFlow(activity, purchaseParams)
        })
    }

    private fun queryPurchases() {
        disposables.add(checkClient().subscribe({
            val result = billingClient!!.queryPurchases(INAPP)
            if (billingClient == null || result.responseCode != BillingResponse.OK) return@subscribe

            val purchasesManager = PurchasesManager.instance
            purchasesManager.onPurchasesQueried(result.responseCode, result.purchasesList)
        }, errorHandler))
    }

    private fun consume(purchaseToken: String) {
        disposables.add(checkClient().subscribe({ billingClient?.consumeAsync(purchaseToken) { _, _ -> } }, errorHandler))
    }

    private fun consumeAll() {
        disposables.add(checkClient().subscribe({
            PurchasesManager.instance.clearPurchases()
            val result = billingClient!!.queryPurchases(INAPP)
            if (billingClient == null || result.responseCode != BillingResponse.OK) return@subscribe
            for (item in result.purchasesList) consume(item.purchaseToken)
        }, errorHandler))
    }

    private fun checkClient(): Completable =
            Completable.create(BillingExecutor()).timeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)

    /**
     * Clear the resources
     */
    fun destroy() {
        try {
            if (billingClient != null) billingClient!!.endConnection()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        billingClient = null
        disposables.dispose()
    }

    inner class BillingExecutor internal constructor() : CompletableOnSubscribe {

        override fun subscribe(emitter: CompletableEmitter) {
            if (isServiceConnected) {
                emitter.onComplete()
                return
            }

            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                    isServiceConnected = billingResponseCode == BillingResponse.OK

                    if (isServiceConnected) emitter.onComplete()
                    else emitter.onError(Exception("Inititalization Exception"))
                }

                override fun onBillingServiceDisconnected() {
                    isServiceConnected = false
                }
            })
        }
    }

    companion object {

        private const val CONNECTION_TIMEOUT = 5
    }
}

