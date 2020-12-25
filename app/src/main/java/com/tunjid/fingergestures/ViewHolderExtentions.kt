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

package com.tunjid.fingergestures

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.core.content.unwrapActivity

private class ViewHolderLifecycleOwner(viewHolder: RecyclerView.ViewHolder) : LifecycleOwner {
    private var attachedToParent = false
    private val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry

    init {
        // When the ViewHolder itself is attached or recycled
        viewHolder.itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) =
                    registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

            override fun onViewAttachedToWindow(v: View?) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

                if (attachedToParent) return
                val parentView = (viewHolder.itemView.parent as? View) ?: return

                // When the parent RecyclerView is detached
                parentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewDetachedFromWindow(v: View?) {
                        v?.removeOnAttachStateChangeListener(this)
                        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    }

                    override fun onViewAttachedToWindow(v: View?) = Unit
                })
                attachedToParent = true
            }
        })

        if (viewHolder.itemView.isAttachedToWindow) registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val activityLifecycle = (viewHolder.itemView.context.unwrapActivity as? FragmentActivity)?.lifecycle
        val activityLifecycleObserver = LifecycleEventObserver { _, event -> registry.handleLifecycleEvent(event) }

        activityLifecycle?.addObserver(activityLifecycleObserver)
        registry.addObserver(LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_DESTROY) activityLifecycle?.removeObserver(activityLifecycleObserver) })
    }
}

val RecyclerView.ViewHolder.lifecycleOwner: LifecycleOwner
    get() = ViewHolderLifecycleOwner(this)