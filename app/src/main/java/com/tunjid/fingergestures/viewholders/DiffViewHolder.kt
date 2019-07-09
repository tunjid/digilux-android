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
import androidx.recyclerview.widget.DiffUtil
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.adapters.AppAdapter
import java.util.*

abstract class DiffViewHolder<T> internal constructor(
        itemView: View,
        protected val items: List<T>,
        listener: AppAdapter.AppAdapterListener)
    : AppViewHolder(itemView, listener) {

    private val listManager: ListManager<*, Void>

    internal abstract val sizeCacheKey: String
    internal abstract val listSupplier: () -> List<T>

    init {
        listManager = createListManager(itemView)
    }

    internal fun diff() {
        disposables.add(App.diff(items, listSupplier, { item: T -> this.diffHash(item) }).subscribe(this::onDiff, Throwable::printStackTrace))
    }

    protected open fun diffHash(item: T): String = item.toString()

    internal abstract fun createListManager(itemView: View): ListManager<*, Void>

    private fun onDiff(diffResult: DiffUtil.DiffResult) {
        val key = sizeCacheKey
        val oldSize = sizeMap.getOrPut(key, { 0 })
        val newSize = items.size

        if (oldSize != newSize) beginDelayedTransition(itemView as ViewGroup, AutoTransition())
        sizeMap[key] = newSize

        listManager.onDiff(diffResult)
    }

    companion object {

        private val sizeMap = HashMap<String, Int>()

        fun onActivityDestroyed() = sizeMap.clear()
    }
}
