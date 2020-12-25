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
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.PopUpGestureConsumer.Companion.ACTION_ACCESSIBILITY_BUTTON
import com.tunjid.fingergestures.PopUpGestureConsumer.Companion.ACTION_SHOW_POPUP
import com.tunjid.fingergestures.PopUpGestureConsumer.Companion.EXTRA_SHOWS_ACCESSIBILITY_BUTTON
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer.Companion.ACTION_EXPAND_VOLUME_CONTROLS
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.Companion.ACTION_SCREEN_DIMMER_CHANGED
import com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.Companion.ACTION_TOGGLE_DOCK
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.GlobalActionGestureConsumer.Companion.ACTION_GLOBAL_ACTION
import com.tunjid.fingergestures.gestureconsumers.GlobalActionGestureConsumer.Companion.EXTRA_GLOBAL_ACTION
import com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.Companion.ACTION_NOTIFICATION_DOWN
import com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.Companion.ACTION_NOTIFICATION_TOGGLE
import com.tunjid.fingergestures.gestureconsumers.NotificationGestureConsumer.Companion.ACTION_NOTIFICATION_UP
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.Companion.ACTION_WATCH_WINDOW_CHANGES
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.Companion.EXTRA_WATCHES_WINDOWS
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit.MILLISECONDS

class FingerGestureService : AccessibilityService() {

    private var overlayView: View? = null

    private var gestureThread: HandlerThread? = null
    private var broadcastDisposable: Disposable? = null

    private val accessibilityButtonCallback = object : AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) =
                PopUpGestureConsumer.instance.showPopup()
    }

    private val screenWakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onIntentReceived(intent)
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

        if (gestureThread != null) gestureThread?.quitSafely()

        gestureThread = HandlerThread("GestureThread").apply {
            start()
            fingerprintGestureController.registerFingerprintGestureCallback(GestureMapper.instance, Handler(looper))
        }

        BackgroundManager.instance.restoreWallpaperChange()

        subscribeToBroadcasts()
        registerReceiver(screenWakeReceiver, IntentFilter(ACTION_SCREEN_ON))
        setWatchesWindows(RotationGestureConsumer.instance.canAutoRotate())
        setShowsAccessibilityButton(PopUpGestureConsumer.instance.hasAccessibilityButton())
    }

    override fun onDestroy() {
        unregisterReceiver(screenWakeReceiver)
        fingerprintGestureController.unregisterFingerprintGestureCallback(GestureMapper.instance)
        accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)

        if (broadcastDisposable != null) broadcastDisposable!!.dispose()
        if (gestureThread != null) gestureThread!!.quitSafely()
        gestureThread = null

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        RotationGestureConsumer.instance.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {}

    private fun expandQuickSettings() {
        val info = findNode(rootInActiveWindow, getSystemUiString(RESOURCE_EXPAND_QUICK_SETTINGS, DEFAULT_EXPAND_QUICK_SETTINGS))
        info?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: swipeDown()
    }

    private fun notificationsShowing(): Boolean {
        val info = this@FingerGestureService.rootInActiveWindow
        return info != null && info.packageName != null && ANDROID_SYSTEM_UI_PACKAGE == info.packageName.toString()
    }

    private fun expandAudioControls() {
        windows
                .asSequence()
                .map(AccessibilityWindowInfo::getRoot)
                .filterNotNull()
                .map { nodeInfo -> findNode(nodeInfo, getSystemUiString(RESOURCE_EXPAND_VOLUME_CONTROLS, DEFAULT_EXPAND_VOLUME)) }
                .filterNotNull()
                .firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

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

    private fun adjustDimmer() {
        val windowManager = getSystemService(WindowManager::class.java) ?: return

        val brightnessGestureConsumer = BrightnessGestureConsumer.instance
        val dimAmount = brightnessGestureConsumer.screenDimmerPercentPreference.value

        if (brightnessGestureConsumer.shouldShowDimmer()) {
            val params: WindowManager.LayoutParams =
                    if (overlayView == null) getLayoutParams(windowManager)
                    else overlayView?.layoutParams as WindowManager.LayoutParams

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

        if (enabled)
            info.flags = info.flags or FLAG_REQUEST_ACCESSIBILITY_BUTTON
        else
            info.flags = info.flags and FLAG_REQUEST_ACCESSIBILITY_BUTTON.inv()

        serviceInfo = info
        controller.registerAccessibilityButtonCallback(accessibilityButtonCallback)
    }

    private fun subscribeToBroadcasts() {
        App.withApp { app ->
            broadcastDisposable = app.broadcasts()
                    .filter(this::intentMatches)
                    .subscribe(this::onIntentReceived) { error ->
                        error.printStackTrace()
                        subscribeToBroadcasts() // Resubscribe on error
                    }
        }
    }

    private fun onIntentReceived(intent: Intent) {
        when (intent.action ?: return) {
            ACTION_SCREEN_ON -> BrightnessGestureConsumer.instance.onScreenTurnedOn()
            ACTION_NOTIFICATION_DOWN -> if (notificationsShowing())
                expandQuickSettings()
            else
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ACTION_NOTIFICATION_UP -> closeNotifications()
            ACTION_NOTIFICATION_TOGGLE -> when {
                isQuickSettingsShowing -> closeNotifications()
                notificationsShowing() -> expandQuickSettings()
                else -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            ACTION_EXPAND_VOLUME_CONTROLS -> expandAudioControls()
            ACTION_SCREEN_DIMMER_CHANGED -> adjustDimmer()
            ACTION_TOGGLE_DOCK -> {
                performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                App.delay(DELAY.toLong(), MILLISECONDS) { performGlobalAction(GLOBAL_ACTION_RECENTS) }
            }
            ACTION_WATCH_WINDOW_CHANGES -> setWatchesWindows(intent.getBooleanExtra(EXTRA_WATCHES_WINDOWS, false))
            ACTION_ACCESSIBILITY_BUTTON -> setShowsAccessibilityButton(intent.getBooleanExtra(EXTRA_SHOWS_ACCESSIBILITY_BUTTON, false))
            ACTION_GLOBAL_ACTION -> {
                val globalAction = intent.getIntExtra(EXTRA_GLOBAL_ACTION, -1)
                if (globalAction != -1) performGlobalAction(globalAction)
            }
            ACTION_SHOW_POPUP -> PopUpGestureConsumer.instance.showPopup()
        }
    }

    private fun intentMatches(intent: Intent): Boolean = when (intent.action) {
        ACTION_SCREEN_DIMMER_CHANGED,
        ACTION_EXPAND_VOLUME_CONTROLS,
        ACTION_ACCESSIBILITY_BUTTON,
        ACTION_WATCH_WINDOW_CHANGES,
        ACTION_NOTIFICATION_TOGGLE,
        ACTION_NOTIFICATION_DOWN,
        ACTION_NOTIFICATION_UP,
        ACTION_GLOBAL_ACTION,
        ACTION_TOGGLE_DOCK,
        ACTION_SHOW_POPUP,
        ACTION_SCREEN_ON -> true
        else -> false
    }


    private fun getSystemUiString(resourceID: String, defaultValue: String): String {
        val resources = systemUiResources ?: return defaultValue
        val id = resources.getIdentifier(resourceID, STRING_RESOURCE, ANDROID_SYSTEM_UI_PACKAGE)

        return if (id == INVALID_RESOURCE)
            defaultValue
        else
            resources.getString(id)
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

        const val ACTION_SHOW_SNACK_BAR = "action show snack bar"
        const val EXTRA_SHOW_SNACK_BAR = " extra show snack bar"
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
