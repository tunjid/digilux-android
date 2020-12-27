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

import android.content.Intent
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderScreenDimmerBinding
import com.tunjid.fingergestures.models.Input

private var BindingViewHolder<ViewholderScreenDimmerBinding>.item by viewHolderDelegate<Item.ScreenDimmer>()

fun ViewGroup.screenDimmer() = viewHolderFrom(ViewholderScreenDimmerBinding::inflate).apply {
    itemView.setOnClickListener {
        if (!item.dimmerState.visible) {
            if (item.dimmerState.enabled) it.context.startActivity(Intent(ACTION_MANAGE_OVERLAY_PERMISSION))
            else item.input.accept(Input.UiInteraction.GoPremium(R.string.premium_prompt_dimmer))
        }
    }
    binding.toggle.setOnCheckedChangeListener { _, isChecked -> item.consumer(isChecked) }
}

fun BindingViewHolder<ViewholderScreenDimmerBinding>.bind(item: Item.ScreenDimmer) = binding.run {
    this@bind.item = item

    // For reference:
    // enabled: is premium
    // visible: has overlay permissions
    // checked: user wants to use dimmer

    val dimmerState = item.dimmerState
    goToSettings.isVisible = !dimmerState.visible

    toggle.isEnabled = dimmerState.enabled
    toggle.isVisible = dimmerState.visible
    toggle.setText(if (dimmerState.enabled) R.string.screen_dimmer_toggle else R.string.go_premium_text)
    toggle.isChecked = dimmerState.checked
}
