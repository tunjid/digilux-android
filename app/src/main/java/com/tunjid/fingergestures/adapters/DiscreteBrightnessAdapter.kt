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
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.viewholders.DiscreteItemViewHolder


class DiscreteBrightnessAdapter(
        items: List<String>,
        listener: BrightnessValueClickListener
) : DiffAdapter<DiscreteItemViewHolder, DiscreteBrightnessAdapter.BrightnessValueClickListener, String>(items, listener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscreteItemViewHolder =
            DiscreteItemViewHolder(getItemView(R.layout.viewholder_chip, parent), adapterListener)

    override fun onBindViewHolder(holder: DiscreteItemViewHolder, position: Int) =
            holder.bind(list[position])

    interface BrightnessValueClickListener : AdapterListener {
        fun onDiscreteBrightnessClicked(discreteValue: String)
    }
}
