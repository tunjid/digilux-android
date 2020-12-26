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
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.databinding.ViewholderSimpleTextBinding
import com.tunjid.fingergestures.viewmodels.Input

private var BindingViewHolder<ViewholderSimpleTextBinding>.item by viewHolderDelegate<Item.AdFree>()

fun ViewGroup.adFree() = viewHolderFrom(ViewholderSimpleTextBinding::inflate)

fun BindingViewHolder<ViewholderSimpleTextBinding>.bind(item: Item.AdFree) = binding.run {
    this@bind.item = item

    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
        title,
        if (item.hasAds) R.drawable.ic_attach_money_white_24dp
        else R.drawable.ic_check_white_24dp,
        0,
        0,
        0)

    title.setText(when {
        item.hasAds -> R.string.go_ad_free_title
        item.notPremium -> R.string.ad_free_no_ad_user
        else -> R.string.ad_free_premium_user
    })

    val goAdFree = { _: View ->
        if (item.hasAds) showPurchaseOptions()
        else showRemainingPurchaseOption()
    }

    itemView.setOnClickListener(if (item.notAdFree || item.notPremium) goAdFree else null)
}

private fun BindingViewHolder<ViewholderSimpleTextBinding>.showPurchaseOptions() {
    val context = itemView.context
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.purchase_options)
        .setItems(R.array.purchase_options
        ) { _, index ->
            item.input.accept(Input.UiInteraction.Purchase(
                if (index == 0) PurchasesManager.Sku.AdFree
                else PurchasesManager.Sku.Premium
            ))
        }
        .show()
}

private fun BindingViewHolder<ViewholderSimpleTextBinding>.showRemainingPurchaseOption() {
    val action = {
        item.input.accept(Input.UiInteraction.Purchase(
            if (item.notPremium) PurchasesManager.Sku.Premium
            else PurchasesManager.Sku.AdFree
        ))
    }

    @StringRes val title =
        if (item.notPremium) R.string.go_premium_title
        else R.string.premium_generosity_title

    @StringRes val description =
        if (item.notPremium) R.string.go_premium_confirmation
        else R.string.premium_generosity_confirmation

    val context = itemView.context
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(description)
        .setPositiveButton(R.string.continue_text) { _, _ -> action.invoke() }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
}

