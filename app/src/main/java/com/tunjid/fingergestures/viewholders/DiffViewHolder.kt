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

import android.transition.AutoTransition
import android.transition.TransitionManager.beginDelayedTransition
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.lifecycleOwner
import java.util.*

abstract class DiffViewHolder<T> internal constructor(
        itemView: View,
        protected val items: LiveData<List<T>>,
        listener: AppAdapterListener)
    : AppViewHolder(itemView, listener) {

    private val listAdapter: ListAdapter<T, *>

    internal abstract val sizeCacheKey: String
    internal abstract val listSupplier: () -> List<T>

    init {
        listAdapter = setupRecyclerView(itemView.findViewById(R.id.item_list))
        items.observe(lifecycleOwner) {
            val key = sizeCacheKey
            val oldSize = sizeMap.getOrPut(key, { 0 })
            val newSize = it.size

            if (oldSize != newSize) beginDelayedTransition(itemView as ViewGroup, AutoTransition())
            sizeMap[key] = newSize

            listAdapter.submitList(it)
        }
    }

    internal abstract fun setupRecyclerView(recyclerView: RecyclerView): ListAdapter<T, *>

    companion object {

        private val sizeMap = HashMap<String, Int>()

        fun onActivityDestroyed() = sizeMap.clear()
    }
}
