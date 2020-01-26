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

import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.fingergestures.R

/**
 * A Proxy Adapter that adds extra items to the bottom of the actual adapter for over scrolling
 * to easily compensate for going edge to edge
 */
class MasqueradeAdapter<T : RecyclerView.ViewHolder>(
        private val proxyAdapter: RecyclerView.Adapter<T>,
        private val extras: Int)
    : RecyclerView.Adapter<T>() {

    init {
        setHasStableIds(proxyAdapter.hasStableIds())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T =
            proxyAdapter.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = proxyAdapter.itemCount + extras

    override fun getItemId(position: Int): Long =
            if (position < proxyAdapter.itemCount) proxyAdapter.getItemId(position)
            else Long.MAX_VALUE - (position - proxyAdapter.itemCount)

    override fun getItemViewType(position: Int): Int =
            if (position < proxyAdapter.itemCount) proxyAdapter.getItemViewType(position)
            else super.getItemViewType(position)

    override fun onBindViewHolder(holder: T, position: Int) {
        val isFromProxy = position < proxyAdapter.itemCount
        holder.itemView.adjustSpacers(isFromProxy)

        if (isFromProxy) proxyAdapter.onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: T, position: Int, payloads: MutableList<Any>) {
        val isFromProxy = position < proxyAdapter.itemCount
        holder.itemView.adjustSpacers(isFromProxy)

        if (isFromProxy) proxyAdapter.onBindViewHolder(holder, position, payloads)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) = super.unregisterAdapterDataObserver(observer)
            .apply { proxyAdapter.unregisterAdapterDataObserver(observer) }

    override fun onViewDetachedFromWindow(holder: T) = proxyAdapter.onViewDetachedFromWindow(holder)

    override fun setHasStableIds(hasStableIds: Boolean) = super.setHasStableIds(hasStableIds)
            .apply { proxyAdapter.setHasStableIds(hasStableIds) }

    override fun onFailedToRecycleView(holder: T): Boolean =
            proxyAdapter.onFailedToRecycleView(holder)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) =
            proxyAdapter.onAttachedToRecyclerView(recyclerView)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) =
            proxyAdapter.onDetachedFromRecyclerView(recyclerView)

    override fun onViewRecycled(holder: T) = proxyAdapter.onViewRecycled(holder)

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) = super.registerAdapterDataObserver(observer)
            .apply { proxyAdapter.registerAdapterDataObserver(observer) }

    override fun onViewAttachedToWindow(holder: T) = proxyAdapter.onViewAttachedToWindow(holder)

    private fun View.adjustSpacers(isFromProxy: Boolean) {
        visibility = if (isFromProxy) VISIBLE else INVISIBLE
        layoutParams.height = if (isFromProxy) RecyclerView.LayoutParams.WRAP_CONTENT else context.resources.getDimensionPixelSize(R.dimen.octuple_margin)
        (this as? ViewGroup)?.forEach { it.visibility = if (isFromProxy) VISIBLE else GONE }
    }

}

fun <VH : RecyclerView.ViewHolder> RecyclerView.Adapter<VH>.padded(extras: Int = 1) =
        MasqueradeAdapter(this, extras)