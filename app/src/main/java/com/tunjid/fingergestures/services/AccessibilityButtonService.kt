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

package com.tunjid.fingergestures.services

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Path
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK
import android.view.accessibility.AccessibilityWindowInfo
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.di.dagger
import com.tunjid.fingergestures.filterIsInstance
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.models.ignore
import com.tunjid.fingergestures.ui.dimmer.onOverlayChanged
import com.tunjid.fingergestures.ui.dimmer.overlayState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.TimeUnit.MILLISECONDS

class AccessibilityButtonService : AccessibilityService() {

    private val accessibilityButtonCallback = object : AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) =
            dagger.appComponent.broadcaster().invoke(Broadcast.ShowPopUp)
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(accessibilityButtonCallback)
    }

    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit
}
