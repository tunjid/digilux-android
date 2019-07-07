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

package com.tunjid.fingergestures.viewholders

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.TextViewCompat
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.billing.PurchasesManager

class AdFreeViewHolder(itemView: View, listener: AppAdapter.AppAdapterListener) : AppViewHolder(itemView, listener) {

    private val purchasesManager: PurchasesManager = PurchasesManager.instance
    private val title: TextView = itemView.findViewById(R.id.title)

    override fun bind() {
        super.bind()
        val hasAds = purchasesManager.hasAds()
        val notAdFree = purchasesManager.hasNotGoneAdFree()
        val notPremium = purchasesManager.isNotPremium

        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                title,
                if (hasAds) R.drawable.ic_attach_money_white_24dp
                else R.drawable.ic_check_white_24dp,
                0,
                0,
                0)

        title.setText(when {
            hasAds -> R.string.go_ad_free_title
            notPremium -> R.string.ad_free_no_ad_user
            else -> R.string.ad_free_premium_user
        })

        val goAdFree = { _: View ->
            if (hasAds) showPurchaseOptions()
            else showRemainingPurchaseOption()
        }

        itemView.setOnClickListener(if (notAdFree || notPremium) goAdFree else null)
    }

    private fun showPurchaseOptions() {
        val context = itemView.context
        AlertDialog.Builder(context)
                .setTitle(R.string.purchase_options)
                .setItems(R.array.purchase_options
                ) { _, index ->
                    adapterListener.purchase(
                            if (index == 0) PurchasesManager.AD_FREE_SKU
                            else PurchasesManager.PREMIUM_SKU)
                }
                .show()
    }

    private fun showRemainingPurchaseOption() {
        val notPremium = purchasesManager.isNotPremium
        val action = {
            if (notPremium) adapterListener.purchase(PurchasesManager.PREMIUM_SKU)
            else adapterListener.purchase(PurchasesManager.AD_FREE_SKU)
        }

        @StringRes val title =
                if (notPremium) R.string.go_premium_title
                else R.string.premium_generosity_title

        @StringRes val description =
                if (notPremium) R.string.go_premium_confirmation
                else R.string.premium_generosity_confirmation

        val context = itemView.context
        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(description)
                .setPositiveButton(R.string.continue_text) { _, _ -> action.invoke() }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}
