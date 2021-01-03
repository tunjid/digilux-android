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

package com.tunjid.fingergestures.ui.dimmer

import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.getSystemService
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.databinding.WindowOverlayBinding
import com.tunjid.fingergestures.di.dagger
import com.tunjid.fingergestures.filterIsInstance
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.services.FingerGestureService
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers

private val Context.windowManager: WindowManager? get() = getSystemService()

data class State(
    val dimPercent: Float = 0f,
    val binding: WindowOverlayBinding? = null
)

private val State.hasView get() = dimPercent > 0f

fun FingerGestureService.overlayState(): Flowable<State> =
    dagger.appComponent.broadcasts().filterIsInstance<Broadcast.Overlay.ScreenDimmerChanged>()
        .map(Broadcast.Overlay.ScreenDimmerChanged::percent)
        .startWith(0f)
        .map(::State)
        .distinctUntilChanged()
        .observeOn(AndroidSchedulers.mainThread())
        .scan { prev, curr ->
            curr.copy(binding = when {
                curr.hasView -> prev.binding ?: createBinding()
                else -> removeBinding(prev.binding)
            })
        }
        .distinctUntilChanged()

fun FingerGestureService.onOverlayChanged(state: State) {
    val binding = state.binding ?: return
    val dimPercent = state.dimPercent
    val params: WindowManager.LayoutParams = binding.root.layoutParams
        as? WindowManager.LayoutParams
        ?: return

    params.alpha = 0.1f
    params.dimAmount = dimPercent
    windowManager?.updateViewLayout(binding.root, params)
}

private fun Context.createBinding(): WindowOverlayBinding? {
    println("Adding overlay")

    val binding = getSystemService<LayoutInflater>()
        ?.cloneInContext(ContextThemeWrapper(this, R.style.AppTheme))
        ?.let(WindowOverlayBinding::inflate)
        ?: return null

    windowManager?.addView(binding.root, WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
        PixelFormat.TRANSLUCENT))
        ?: return null

    println("Added overlay")
    return binding
}

private fun Context.removeBinding(old: WindowOverlayBinding?): WindowOverlayBinding? {
    println("Removed overlay")
    old?.let { windowManager?.removeView(it.root) }
    return null
}
