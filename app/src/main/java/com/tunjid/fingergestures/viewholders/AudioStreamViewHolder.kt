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
import android.widget.RadioButton
import androidx.core.view.children
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.databinding.ViewholderAudioStreamTypeBinding
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.ui.main.Input

private var BindingViewHolder<ViewholderAudioStreamTypeBinding>.item by viewHolderDelegate<Item.AudioStream>()

fun ViewGroup.audioStream() = viewHolderFrom(ViewholderAudioStreamTypeBinding::inflate).apply {
    binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
        item.consumer(AudioGestureConsumer.Stream.forId(checkedId).type)
    }
}

fun BindingViewHolder<ViewholderAudioStreamTypeBinding>.bind(item: Item.AudioStream) = binding.run {
    this@bind.item = item

    if (!item.hasDoNotDisturbAccess) item.input.accept(Input.Permission.Request.DoNotDisturb)

    radioGroup.check(item.stream.id)

    radioGroup.children
        .filterIsInstance<RadioButton>()
        .forEach { radioButton ->
            radioButton.isEnabled = item.hasDoNotDisturbAccess
            radioButton.text = item.titleFunction(radioButton.id)
        }
}
