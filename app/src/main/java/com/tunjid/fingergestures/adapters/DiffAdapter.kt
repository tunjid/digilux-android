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

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder

abstract class DiffAdapter<V : InteractiveViewHolder<T>, T : InteractiveAdapter.AdapterListener, S> internal constructor(internal val list: List<S>, listener: T) : InteractiveAdapter<V, T>(listener) {

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long = list[position].hashCode().toLong()
}
