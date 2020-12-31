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
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK
import android.view.accessibility.AccessibilityWindowInfo
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.di.dagger
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.models.ignore
import com.tunjid.fingergestures.filterIsInstance
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.TimeUnit.MILLISECONDS

class FingerGestureService : AccessibilityService() {

    private val broadcastDisposables get() = dagger.appComponent.appDisposable()
    private val dependencies get() = dagger.appComponent.dependencies()
    private val gestureConsumers get() = dependencies.gestureConsumers
    private val gestureMapper get() = dependencies.gestureMapper

    private var overlayView: View? = null

    private val gestureThread by lazy { HandlerThread("GestureThread").also(HandlerThread::start) }

    private val accessibilityButtonCallback = object : AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) =
            gestureConsumers.popUp.showPopup()
    }

    private val screenWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = gestureConsumers.brightness.onScreenTurnedOn()
    }

    private val isQuickSettingsShowing: Boolean
        get() = findNode(rootInActiveWindow, getSystemUiString(RESOURCE_EDIT_QUICK_SETTINGS, DEFAULT_EDIT_QUICK_SETTINGS)) != null

    private val systemUiResources: Resources?
        get() = try {
            packageManager.getResourcesForApplication(ANDROID_SYSTEM_UI_PACKAGE)
        } catch (e: Exception) {
            null
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        fingerprintGestureController.registerFingerprintGestureCallback(gestureMapper, Handler(gestureThread.looper))
        dependencies.backgroundManager.restoreWallpaperChange()

        subscribeToBroadcasts()
        registerReceiver(screenWakeReceiver, IntentFilter(ACTION_SCREEN_ON))
        setWatchesWindows(gestureConsumers.rotation.autoRotatePreference.value)
        setShowsAccessibilityButton(gestureConsumers.popUp.accessibilityButtonEnabledPreference.value)
    }

    override fun onDestroy() {
        unregisterReceiver(screenWakeReceiver)
        fingerprintGestureController.unregisterFingerprintGestureCallback(gestureMapper)
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)

        broadcastDisposables.clear()
        gestureThread.quitSafely()

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) =
        gestureConsumers.rotation.onAccessibilityEvent(event)

    override fun onInterrupt() = Unit

    private fun expandQuickSettings() {
        val info = findNode(rootInActiveWindow, getSystemUiString(RESOURCE_EXPAND_QUICK_SETTINGS, DEFAULT_EXPAND_QUICK_SETTINGS))
        info?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: swipeDown()
    }

    private fun notificationsShowing(): Boolean {
        val info = this@FingerGestureService.rootInActiveWindow
        return info != null && info.packageName != null && ANDROID_SYSTEM_UI_PACKAGE == info.packageName.toString()
    }

    private fun expandAudioControls() =
        windows
            .asSequence()
            .map(AccessibilityWindowInfo::getRoot)
            .filterNotNull()
            .map { nodeInfo -> findNode(nodeInfo, getSystemUiString(RESOURCE_EXPAND_VOLUME_CONTROLS, DEFAULT_EXPAND_VOLUME)) }
            .filterNotNull()
            .firstOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            .ignore

    private fun closeNotifications() {
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        closeIntent.setPackage(ANDROID_SYSTEM_UI_PACKAGE)
        sendBroadcast(closeIntent)
    }

    private fun swipeDown() {
        val displayMetrics = resources.displayMetrics

        val gestureBuilder = GestureDescription.Builder()
        val path = Path()

        path.moveTo(0f, 0f)
        path.lineTo(displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat())
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 20))

        dispatchGesture(gestureBuilder.build(), object : AccessibilityService.GestureResultCallback() {

        }, null)
    }

    private fun adjustDimmer(broadcast: Broadcast.Service.ScreenDimmerChanged) {
        val windowManager = getSystemService(WindowManager::class.java) ?: return

        val brightnessGestureConsumer = gestureConsumers.brightness
        val dimAmount = broadcast.percent

        if (brightnessGestureConsumer.shouldShowDimmer()) {
            val params: WindowManager.LayoutParams =
                if (overlayView == null) getLayoutParams(windowManager)
                else overlayView?.layoutParams as? WindowManager.LayoutParams ?: return

            params.alpha = 0.1f
            params.dimAmount = dimAmount
            windowManager.updateViewLayout(overlayView, params)
        } else if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    @SuppressLint("InflateParams")
    private fun getLayoutParams(windowManager: WindowManager): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT)

        val inflater = getSystemService(LayoutInflater::class.java) ?: return params

        overlayView = inflater.inflate(R.layout.window_overlay, null)
        windowManager.addView(overlayView, params)

        return params
    }

    private fun setWatchesWindows(enabled: Boolean) {
        val info = serviceInfo
        info.eventTypes = if (enabled) TYPE_WINDOW_STATE_CHANGED else 0
        serviceInfo = info
    }

    private fun setShowsAccessibilityButton(enabled: Boolean) {
        val controller = accessibilityButtonController
        val info = serviceInfo

        info.flags = when {
            enabled -> info.flags or FLAG_REQUEST_ACCESSIBILITY_BUTTON
            else -> info.flags and FLAG_REQUEST_ACCESSIBILITY_BUTTON.inv()
        }

        serviceInfo = info
        controller.registerAccessibilityButtonCallback(accessibilityButtonCallback)
    }

    private fun subscribeToBroadcasts() {
        dagger.appComponent.broadcasts()
            .filterIsInstance<Broadcast.Service>()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::onBroadcastReceived) { error ->
                error.printStackTrace()
                subscribeToBroadcasts() // Resubscribe on error
            }
            .addTo(broadcastDisposables)
    }

    private fun onBroadcastReceived(broadcast: Broadcast.Service) = when (broadcast) {
        Broadcast.Service.ExpandVolumeControls -> expandAudioControls()
        Broadcast.Service.ShowPopUp -> gestureConsumers.popUp.showPopup()
        Broadcast.Service.ToggleDock -> {
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            App.delay(DELAY.toLong(), MILLISECONDS) { performGlobalAction(GLOBAL_ACTION_RECENTS) }
            Unit
        }
        Broadcast.Service.ShadeDown -> when {
            notificationsShowing() -> expandQuickSettings()
            else -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS).ignore
        }
        Broadcast.Service.ShadeUp -> closeNotifications()
        Broadcast.Service.ShadeToggle -> when {
            isQuickSettingsShowing -> closeNotifications()
            notificationsShowing() -> expandQuickSettings()
            else -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS).ignore
        }
        is Broadcast.Service.AccessibilityButtonChanged -> setShowsAccessibilityButton(broadcast.enabled)
        is Broadcast.Service.ScreenDimmerChanged -> adjustDimmer(broadcast)
        is Broadcast.Service.GlobalAction -> {
            val globalAction = broadcast.action
            if (globalAction != -1) performGlobalAction(globalAction).ignore
            else Unit
        }
        is Broadcast.Service.WatchesWindows -> setWatchesWindows(broadcast.enabled)
    }

    private fun getSystemUiString(resourceID: String, defaultValue: String): String {
        val resources = systemUiResources ?: return defaultValue
        val id = resources.getIdentifier(resourceID, STRING_RESOURCE, ANDROID_SYSTEM_UI_PACKAGE)

        return if (id == INVALID_RESOURCE) defaultValue
        else resources.getString(id)
    }

    private fun findNode(info: AccessibilityNodeInfo?, name: String): AccessibilityNodeInfo? {
        if (info == null) return null
        val size = info.childCount

        if (size > 0)
            for (i in 0 until size) {
                val node = findNode(info.getChild(i), name)
                if (node != null) return node
            }
        if (!info.actionList.contains(ACTION_CLICK)) return null

        val contentDescription = info.contentDescription
        if (TextUtils.isEmpty(contentDescription)) return null

        val description = contentDescription.toString()
        return if (description.contains(name)) info else null
    }

    companion object {
        const val ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui"

        private const val RESOURCE_EXPAND_VOLUME_CONTROLS = "accessibility_volume_expand"
        private const val RESOURCE_EXPAND_QUICK_SETTINGS = "accessibility_quick_settings_expand"
        private const val RESOURCE_EDIT_QUICK_SETTINGS = "accessibility_quick_settings_edit"
        private const val DEFAULT_EXPAND_VOLUME = "Expand"
        private const val DEFAULT_EXPAND_QUICK_SETTINGS = "Open quick settings"
        private const val DEFAULT_EDIT_QUICK_SETTINGS = "Edit order of settings"

        private const val STRING_RESOURCE = "string"
        private const val INVALID_RESOURCE = 0
        private const val DELAY = 50
    }
}
