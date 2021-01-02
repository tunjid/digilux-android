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

package com.tunjid.fingergestures.ui

import android.R
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

internal fun AppCompatActivity.dialogLifecycleOwner(): LifecycleOwner {
    val (owner, registry) = with(object : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = registry
    }) { this to registry }

    // Start observing immediately
    registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_RESUME,
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> registry.handleLifecycleEvent(event)
                Lifecycle.Event.ON_DESTROY -> {
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    source.lifecycle.removeObserver(this)
                }
                else -> Unit
            }
        }
    })

    return owner
}

fun Context.divider(): RecyclerView.ItemDecoration {
    val context = this

    val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    val decoration = ContextCompat.getDrawable(context, R.drawable.divider_horizontal_dark)

    if (decoration != null) itemDecoration.setDrawable(decoration)

    return itemDecoration
}

fun ConstraintLayout.updateVerticalBiasFor(viewId: Int) = { verticalBias: Float ->
    val set = ConstraintSet()
    set.clone(this)
    set.setVerticalBias(viewId, verticalBias)
    set.applyTo(this)
}