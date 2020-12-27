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
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.databinding.ViewholderMapperBinding
import com.tunjid.fingergestures.fragments.ActionFragment
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOWN_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.LEFT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.RIGHT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.UP_GESTURE
import com.tunjid.fingergestures.viewmodels.Input

private var BindingViewHolder<ViewholderMapperBinding>.item by viewHolderDelegate<Item.Mapper>()

fun ViewGroup.mapper() = viewHolderFrom(ViewholderMapperBinding::inflate).apply {
    binding.title.setOnClickListener { item.input.accept(Input.UiInteraction.ShowSheet(ActionFragment.directionInstance(item.direction))) }
    binding.subTitle.setOnClickListener {
        item.input.accept(
                if (PurchasesManager.instance.isNotPremium) Input.UiInteraction.GoPremium(R.string.premium_prompt_double_swipe)
                else Input.UiInteraction.ShowSheet(ActionFragment.directionInstance(item.doubleDirection))
        )
    }
}

fun BindingViewHolder<ViewholderMapperBinding>.bind(item: Item.Mapper) = binding.run {
    this@bind.item = item

    when (item.direction) {
        UP_GESTURE -> icon.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp)
        DOWN_GESTURE -> icon.setImageResource(R.drawable.ic_app_dock_24dp)
        LEFT_GESTURE -> icon.setImageResource(R.drawable.ic_chevron_left_white_24dp)
        RIGHT_GESTURE -> icon.setImageResource(R.drawable.ic_chevron_right_white_24dp)
    }

    if (!App.accessibilityServiceEnabled) item.input.accept(Input.Permission.Request.Accessibility)
    if (!App.canWriteToSettings) item.input.accept(Input.Permission.Request.Settings)

    fun getFormattedText(directionName: String, text: String): CharSequence =
            itemView.context.getString(R.string.mapper_format).formatSpanned(
                    directionName.bold(),
                    text
            )

    title.text = getFormattedText(item.gesturePair.singleGestureName, item.gesturePair.singleActionName)
    subTitle.text = getFormattedText(item.gesturePair.doubleGestureName, item.gesturePair.doubleActionName)
}
