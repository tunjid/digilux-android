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

package com.tunjid.fingergestures.adapters

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.viewholders.ActionViewHolder


class ActionAdapter(
        private val isHorizontal: Boolean,
        private val showsText: Boolean,
        list: List<Int>, listener: ActionClickListener
) : DiffAdapter<ActionViewHolder, ActionAdapter.ActionClickListener, Int>(list, listener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        @LayoutRes val layoutRes = if (isHorizontal) R.layout.viewholder_action_horizontal else R.layout.viewholder_action_vertical
        return ActionViewHolder(showsText, getItemView(layoutRes, parent), adapterListener)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) = holder.bind(list[position])

    interface ActionClickListener : AdapterListener {
        fun onActionClicked(@GestureConsumer.GestureAction actionRes: Int)
    }
}
