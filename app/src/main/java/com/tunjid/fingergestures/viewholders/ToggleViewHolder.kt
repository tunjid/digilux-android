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
import android.widget.Switch
import androidx.annotation.StringRes
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderToggleBinding

private var BindingViewHolder<ViewholderToggleBinding>.item by viewHolderDelegate<Item.Toggle>()

fun ViewGroup.toggle() = viewHolderFrom(ViewholderToggleBinding::inflate).apply {
    binding.toggle.setOnClickListener { item.consumer.invoke(binding.toggle.isChecked) }
}

fun BindingViewHolder<ViewholderToggleBinding>.bind(item: Item.Toggle) = binding.run {
    this@bind.item = item
    toggle.setText(item.titleRes)
    toggle.isChecked = item.isChecked
}

class ToggleViewHolder(
    itemView: View,
    @StringRes titleRes: Int,
    supplier: () -> Boolean,
    consumer: (Boolean) -> Unit
) : AppViewHolder(itemView) {

    init {
        val toggle = itemView.findViewById<Switch>(R.id.toggle)
        toggle.setText(titleRes)
        toggle.setOnClickListener { consumer.invoke(toggle.isChecked) }
        disposables.add(App.backgroundToMain(supplier).subscribe((toggle::setChecked), Throwable::printStackTrace))
    }
}
