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

package com.tunjid.fingergestures.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.di.AppDependencies
import com.tunjid.fingergestures.gestureconsumers.*
import com.tunjid.fingergestures.managers.PurchasesManager
import com.tunjid.fingergestures.models.*
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainViewModel @Inject constructor(
    @AppContext private val context: Context,
    override val dependencies: AppDependencies,
    private val broadcaster: AppBroadcaster,
    broadcasts: Flowable<Broadcast>,
) : ViewModel(), Inputs {

    private val quips = context.resources.getTextArray(R.array.upsell_text)
    private val inputProcessor: PublishProcessor<Input> = PublishProcessor.create()
    private val disposable: CompositeDisposable = CompositeDisposable()

    private val backingState = Flowables.combineLatest(
        dependencies.purchasesManager.shillingState,
        dependencies.purchasesManager.state,
        Flowable.just(context.links),
        broadcasts.filterIsInstance<Broadcast.Prompt>().map { Optional.of(it) }.startWith(Optional.empty()),
        inputProcessor.filterIsInstance<Input.UiInteraction>().startWith(Input.UiInteraction.Default),
        inputProcessor.permissionState,
        inputProcessor.billingState,
        items,
        ::AppState
    )

    val state: LiveData<AppState> = backingState.toLiveData()

    override fun onCleared() {
        disposable.clear()
        state.value?.billingState?.client?.endConnection()
    }

    init {
        val client = BillingClient.newBuilder(context)
            .setListener(dependencies.purchasesManager)
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) =
                accept(Input.Billing.Client(client = client.takeIf { billingResponseCode == BillingClient.BillingResponse.OK }))

            override fun onBillingServiceDisconnected() = accept(Input.Billing.Client(client = null))
        })

        backingState
            .map(AppState::billingState)
            .mapNotNull(BillingState::client)
            .distinctUntilChanged()
            .subscribe {
                val result = it.queryPurchases(BillingClient.SkuType.INAPP)
                if (result.responseCode != BillingClient.BillingResponse.OK) return@subscribe
                dependencies.purchasesManager.onPurchasesQueried(result.responseCode, result.purchasesList)
            }
            .addTo(disposable)

        inputProcessor
            .filterIsInstance<Input.StartTrial>()
            .subscribe { dependencies.purchasesManager.startTrial() }
            .addTo(disposable)

        inputProcessor
            .filterIsInstance<Input.AppResumed>()
            .subscribe { broadcaster(Broadcast.AppResumed) }
            .addTo(disposable)
    }

    override fun accept(input: Input): Unit = inputProcessor.onNext(input)

    private val PurchasesManager.shillingState
        get() = state
            .map(PurchasesManager.State::hasAds)
            .distinctUntilChanged()
            .switchMap {
                if (it) Flowable.merge(
                    inputProcessor.filterIsInstance<Input.Shill>(),
                    Flowable.interval(10, TimeUnit.SECONDS)
                ).scan(0) { index, _ -> (index + 1) % quips.size }
                    .map(quips::get)
                    .map(Shilling::Quip)
                else Flowable.just(Shilling.Calm)
            }

    private val Flowable<Input>.billingState
        get() = filterIsInstance<Input.Billing>()
            .scan(BillingState()) { state, item ->
                when (item) {
                    is Input.Billing.Client -> state.copy(client = item.client)
                    is Input.Billing.Purchase -> when (val client = state.client) {
                        null -> state.copy(prompt = Unique(R.string.billing_not_connected))
                        else -> state.copy(cart = Unique(client to item.sku))
                    }
                }
            }

    private val Flowable<Input>.permissionState
        get() = filterIsInstance<Input.Permission>()
            .scan(PermissionState()) { state, permission ->
                when (permission) {
                    Input.Permission.Request.Storage,
                    Input.Permission.Request.Settings,
                    Input.Permission.Request.Accessibility,
                    Input.Permission.Request.DoNotDisturb -> {
                        if (permission is Input.Permission.Request) {
                            val queue = if (state.queue.contains(permission)) state.queue - permission else state.queue
                            state.copy(queue = queue + permission)
                        } else state
                    }
                    is Input.Permission.Action.Clear -> state.copy(
                        queue = listOf()
                    )
                    is Input.Permission.Action.Clicked -> state.copy(
                        active = state.queue.lastOrNull()?.let(::Unique),
                        queue = state.queue.dropLast(1)
                    )
                    is Input.Permission.Action.Changed -> {
                        val (prompt, shouldRemove) = when (permission.request) {
                            Input.Permission.Request.Storage ->
                                if (context.hasStoragePermission) R.string.storage_permission_granted to true
                                else R.string.storage_permission_denied to false
                            Input.Permission.Request.Settings ->
                                if (context.canWriteToSettings) R.string.settings_permission_granted to true
                                else R.string.settings_permission_denied to false
                            Input.Permission.Request.Accessibility ->
                                if (context.accessibilityServiceEnabled) R.string.accessibility_permission_granted to true
                                else R.string.accessibility_permission_denied to false
                            Input.Permission.Request.DoNotDisturb ->
                                if (context.hasDoNotDisturbAccess) R.string.do_not_disturb_permission_granted to true
                                else R.string.do_not_disturb_permission_denied to false
                        }
                        state.copy(
                            prompt = Unique(prompt),
                            queue = when {
                                shouldRemove -> state.queue - permission.request
                                else -> state.queue
                            }
                        )
                    }
                }
            }

//    private fun consume(purchaseToken: String) {
//        billingClient.consumeAsync(purchaseToken) { _, _ -> }
//    }
//
//    private fun consumeAll() {
//        PurchasesManager.instance.clearPurchases()
//        val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
//        if (billingClient == null || result.responseCode != BillingClient.BillingResponse.OK) return
//        for (item in result.purchasesList) consume(item.purchaseToken)
//    }
}

val Context.links
    get() = listOf(
        TextLink(text = getString(R.string.get_set_icon), link = "https://github.com/tunjid/android-bootstrap"),
        TextLink(text = getString(R.string.rxjava), link = "https://github.com/ReactiveX/RxJava"),
        TextLink(text = getString(R.string.color_picker), link = "https://github.com/QuadFlask/colorpicker"),
        TextLink(text = getString(R.string.image_cropper), link = "https://github.com/ArthurHub/Android-Image-Cropper"),
        TextLink(text = getString(R.string.material_design_icons), link = "https://materialdesignicons.com/"),
        TextLink(text = getString(R.string.android_bootstrap), link = "http://www.myiconfinder.com/getseticons")
    )
