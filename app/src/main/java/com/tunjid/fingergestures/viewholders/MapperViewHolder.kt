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
import com.tunjid.androidx.core.text.bold
import com.tunjid.androidx.core.text.formatSpanned
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.accessibilityServiceEnabled
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.canWriteToSettings
import com.tunjid.fingergestures.databinding.ViewholderMapperBinding
import com.tunjid.fingergestures.ui.popup.ActionFragment
import com.tunjid.fingergestures.gestureconsumers.GestureDirection
import com.tunjid.fingergestures.models.Input

private var BindingViewHolder<ViewholderMapperBinding>.item by viewHolderDelegate<Item.Mapper>()

fun ViewGroup.mapper() = viewHolderFrom(ViewholderMapperBinding::inflate).apply {
    binding.title.setOnClickListener { item.input.accept(Input.UiInteraction.ShowSheet(ActionFragment.directionInstance(item.direction))) }
    binding.subTitle.setOnClickListener {
        item.input.accept(
            if (item.canUseDoubleSwipes) Input.UiInteraction.ShowSheet(ActionFragment.directionInstance(item.doubleDirection))
            else Input.UiInteraction.GoPremium(R.string.premium_prompt_double_swipe)
        )
    }
}

fun BindingViewHolder<ViewholderMapperBinding>.bind(item: Item.Mapper) = binding.run {
    this@bind.item = item

    icon.setImageResource(when (item.direction) {
        GestureDirection.Up,
        GestureDirection.DoubleUp -> R.drawable.ic_keyboard_arrow_up_white_24dp
        GestureDirection.Down,
        GestureDirection.DoubleDown -> R.drawable.ic_app_dock_24dp
        GestureDirection.Left,
        GestureDirection.DoubleLeft -> R.drawable.ic_chevron_left_white_24dp
        GestureDirection.Right,
        GestureDirection.DoubleRight -> R.drawable.ic_chevron_right_white_24dp
    })

    if (!itemView.context.accessibilityServiceEnabled) item.input.accept(Input.Permission.Request.Accessibility)
    if (!itemView.context.canWriteToSettings) item.input.accept(Input.Permission.Request.Settings)

    fun getFormattedText(directionName: String, text: String): CharSequence =
        itemView.context.getString(R.string.mapper_format).formatSpanned(
            directionName.bold(),
            text
        )

    title.text = getFormattedText(item.gesturePair.singleGestureName, item.gesturePair.singleActionName)
    subTitle.text = getFormattedText(item.gesturePair.doubleGestureName, item.gesturePair.doubleActionName)
}
