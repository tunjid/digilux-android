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

package com.tunjid.fingergestures.baseclasses


import androidx.annotation.StringRes

import com.google.android.material.snackbar.Snackbar

import android.view.ViewGroup

import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity
import com.tunjid.androidbootstrap.functions.Consumer
import com.tunjid.androidbootstrap.view.animator.ViewHider
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.billing.BillingManager
import com.tunjid.fingergestures.billing.PurchasesManager

import io.reactivex.disposables.CompositeDisposable

import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.android.billingclient.api.BillingClient.BillingResponse.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponse.OK
import com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponse.SERVICE_UNAVAILABLE

abstract class FingerGestureActivity : BaseActivity() {

    protected lateinit var barHider: ViewHider
    protected lateinit var fabHider: ViewHider
    protected lateinit var coordinator: ViewGroup
    private val disposables = CompositeDisposable()

    private lateinit var billingManager: BillingManager

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        coordinator = findViewById(R.id.container)
    }

    override fun onResume() {
        super.onResume()
        billingManager = BillingManager(applicationContext)
    }

    override fun onStop() {
        billingManager.destroy()
        disposables.dispose()
        super.onStop()
    }

    fun showSnackbar(@StringRes resource: Int) =
            withSnackbar { snackbar -> snackbar.setText(resource);snackbar.show() }

    fun toggleToolbar(visible: Boolean) =
            if (visible) barHider.show()
            else barHider.hide()

    fun purchase(@PurchasesManager.SKU sku: String) {
        if (billingManager == null)
            showSnackbar(R.string.generic_error)
        else
            disposables.add(billingManager.initiatePurchaseFlow(this, sku)
                    .subscribe({ launchStatus ->
                        when (launchStatus) {
                            OK -> {
                            }
                            SERVICE_UNAVAILABLE, SERVICE_DISCONNECTED -> showSnackbar(R.string.billing_not_connected)
                            ITEM_ALREADY_OWNED -> showSnackbar(R.string.billing_you_own_this)
                            else -> showSnackbar(R.string.generic_error)
                        }
                    }, { throwable -> showSnackbar(R.string.generic_error) }))
    }

    protected fun withSnackbar(consumer: (Snackbar) -> Unit) {
        val snackbar = Snackbar.make(coordinator, R.string.app_name, LENGTH_SHORT)
        snackbar.view.setOnApplyWindowInsetsListener { _, insets -> insets }
        consumer.invoke(snackbar)
    }
}
