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

import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderSliderDeltaBinding

private var BindingViewHolder<ViewholderSliderDeltaBinding>.item by viewHolderDelegate<Item.Slider>()
private var BindingViewHolder<ViewholderSliderDeltaBinding>.isTouched by viewHolderDelegate(false)

fun ViewGroup.sliderAdjuster() = viewHolderFrom(ViewholderSliderDeltaBinding::inflate).apply {
    itemView.setOnClickListener { if (item.infoRes != 0) MaterialAlertDialogBuilder(itemView.context).setMessage(item.infoRes).show() }
    binding.seekbar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            isTouched = true
        }

        override fun onStopTrackingTouch(slider: Slider) {
            isTouched = false
            val percentage = slider.value.toInt()
            item.consumer.invoke(percentage)
            binding.value.text = item.function.invoke(percentage)
        }
    })
    binding.seekbar.addOnChangeListener { _, percentage, fromUser ->
        if (fromUser) {
            item.consumer.invoke(percentage.toInt())
            binding.value.text = item.function.invoke(percentage.toInt())
        }
    }
}

fun BindingViewHolder<ViewholderSliderDeltaBinding>.bind(item: Item.Slider) = binding.run {
    this@bind.item = item

    title.setText(item.titleRes)

    val enabled = item.isEnabled

    seekbar.isEnabled = enabled
    if (!isTouched) seekbar.value = item.value.toFloat()

    value.isEnabled = enabled
    value.text = item.function.invoke(item.value)

    if (item.infoRes != 0) title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_info_outline_white_24dp, 0)
}
