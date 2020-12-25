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

package com.tunjid.fingergestures.gestureconsumers


import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import androidx.annotation.StringDef
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.SetManager
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.services.FingerGestureService.Companion.ANDROID_SYSTEM_UI_PACKAGE
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import java.util.*

class RotationGestureConsumer private constructor() : GestureConsumer {

    private val setManager: SetManager<ApplicationInfo> = SetManager(
            Comparator(this::compareApplicationInfo),
            this::canAddToSet,
            this::fromPackageName,
            this::fromApplicationInfo)

    val autoRotatePreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = WATCHES_WINDOW_CONTENT,
        default = false
    )

    val applicationInfoComparator: Comparator<ApplicationInfo>
        get() = Comparator { infoA, infoB -> this.compareApplicationInfo(infoA, infoB) }

    val rotatingApps = setManager.itemsFlowable(ROTATION_APPS)

    val excludedRotatingApps = setManager.itemsFlowable(EXCLUDED_APPS)

    val lastSeenApps
        get() = shifter.flowable.map { it.mapNotNull(this::fromPackageName) }
                .subscribeOn(Schedulers.io())

    private var lastPackageName: String? = null
    private val shifter = Shifter(9)

    private var isAutoRotateOn: Boolean
        get() = App.transformApp({ app ->
            Settings.System.getInt(
                    app.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    DISABLE_AUTO_ROTATION) == ENABLE_AUTO_ROTATION
        }, false)
        set(isOn) {
            App.withApp { app ->
                val enabled = if (isOn) ENABLE_AUTO_ROTATION else DISABLE_AUTO_ROTATION
                Settings.System.putInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION, enabled)
            }
        }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ROTATION_APPS, EXCLUDED_APPS)
    annotation class PersistedSet

    init {
        setManager.addToSet(ANDROID_SYSTEM_UI_PACKAGE, EXCLUDED_APPS)
        App.withApp { app -> setManager.addToSet(app.packageName, EXCLUDED_APPS) }
    }

    override fun onGestureActionTriggered(gestureAction: Int) {
        isAutoRotateOn = !isAutoRotateOn
    }

    override fun accepts(gesture: Int): Boolean = gesture == GestureConsumer.TOGGLE_AUTO_ROTATE

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!App.canWriteToSettings() || event.eventType != TYPE_WINDOW_STATE_CHANGED) return

        val sequence = event.packageName ?: return

        val packageName = sequence.toString()
        if (packageName == lastPackageName) return

        val rotationApps = setManager.getSet(ROTATION_APPS)
        if (rotationApps.isEmpty() || setManager.getSet(EXCLUDED_APPS).contains(packageName))
            return

        lastPackageName = packageName
        shifter + packageName
        isAutoRotateOn = rotationApps.contains(packageName)
    }

    fun getAddText(@PersistedSet preferencesName: String): String = App.transformApp({ app ->
        app.getString(R.string.auto_rotate_add, if (ROTATION_APPS == preferencesName)
            app.getString(R.string.auto_rotate_apps)
        else
            app.getString(R.string.auto_rotate_apps_excluded))
    }, EMPTY_STRING)

    fun getRemoveText(@PersistedSet preferencesName: String): String = App.transformApp({ app ->
        app.getString(R.string.auto_rotate_remove, if (ROTATION_APPS == preferencesName)
            app.getString(R.string.auto_rotate_apps)
        else
            app.getString(R.string.auto_rotate_apps_excluded))
    }, EMPTY_STRING)

    fun isRemovable(packageName: String): Boolean =
            App.transformApp({ app -> ANDROID_SYSTEM_UI_PACKAGE != packageName && app.packageName != packageName }, false)

    fun addToSet(packageName: String, @PersistedSet preferencesName: String): Boolean =
            setManager.addToSet(packageName, preferencesName)

    fun removeFromSet(packageName: String, @PersistedSet preferencesName: String) =
            setManager.removeFromSet(packageName, preferencesName)

    fun canAutoRotate(): Boolean = autoRotatePreference.item

    fun enableWindowContentWatching(enabled: Boolean) {
        App.withApp { app ->
            app.preferences.edit().putBoolean(WATCHES_WINDOW_CONTENT, enabled).apply()

            val intent = Intent(ACTION_WATCH_WINDOW_CHANGES)
            intent.putExtra(EXTRA_WATCHES_WINDOWS, enabled)

            app.broadcast(intent)
        }
    }

    private fun canAddToSet(preferenceName: String): Boolean {
        val set = setManager.getSet(preferenceName)
        val count = set.filter(this::isRemovable).count()
        return count < 2 || PurchasesManager.instance.isPremiumNotTrial
    }

    private fun compareApplicationInfo(infoA: ApplicationInfo, infoB: ApplicationInfo): Int {
        return App.transformApp({ app ->
            val packageManager = app.packageManager
            packageManager.getApplicationLabel(infoA).toString().compareTo(packageManager.getApplicationLabel(infoB).toString())
        }, 0)
    }

    private fun fromApplicationInfo(info: ApplicationInfo): String = info.packageName

    private fun fromPackageName(packageName: String): ApplicationInfo? = App.transformApp { app ->
        try {
            app.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val ENABLE_AUTO_ROTATION = 1
        private const val DISABLE_AUTO_ROTATION = 0

        const val ACTION_WATCH_WINDOW_CHANGES = "com.tunjid.fingergestures.watch_windows"
        const val EXTRA_WATCHES_WINDOWS = "extra watches window content"
        const val EXCLUDED_APPS = "excluded rotation apps"
        const val ROTATION_APPS = "rotation apps"
        private const val WATCHES_WINDOW_CONTENT = "watches window content"
        private const val EMPTY_STRING = ""

        val instance: RotationGestureConsumer by lazy { RotationGestureConsumer() }
    }
}

private class Shifter(private val size: Int) {
    private val backing = arrayOfNulls<String>(size)
    private val watcher = BehaviorProcessor.create<List<String>>()

    val flowable: Flowable<List<String>> = watcher.doOnSubscribe { push() }

    operator fun plus(value: String) {
        System.arraycopy(backing, 0, backing, 1, size - 1)
        backing[0] = value
        if (watcher.hasSubscribers()) push()
    }

    private fun push() = watcher.onNext(backing.filterNotNull())
}