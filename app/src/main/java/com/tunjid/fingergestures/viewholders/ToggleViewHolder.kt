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
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.databinding.ViewholderToggleBinding

private var BindingViewHolder<ViewholderToggleBinding>.item by viewHolderDelegate<Item.Toggle>()

fun ViewGroup.toggle() = viewHolderFrom(ViewholderToggleBinding::inflate).apply {
    binding.toggle.setOnClickListener { item.onChanged.invoke(binding.toggle.isChecked) }
}

fun BindingViewHolder<ViewholderToggleBinding>.bind(item: Item.Toggle) = binding.run {
    this@bind.item = item
    toggle.setText(item.titleRes)
    toggle.isChecked = item.isChecked
}
