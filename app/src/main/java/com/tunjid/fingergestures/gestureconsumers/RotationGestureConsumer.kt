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
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.services.FingerGestureService.Companion.ANDROID_SYSTEM_UI_PACKAGE
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import java.util.*

class RotationGestureConsumer private constructor() : GestureConsumer {

    enum class Preference(override val preferenceName: String) : ListPreference {
        RotatingApps(preferenceName = "rotation apps"),
        NonRotatingApps(preferenceName = "excluded rotation apps"),
    }

    val setManager: SetManager<Preference, ApplicationInfo> = SetManager(
        keys = Preference.values().asIterable(),
        sorter = Comparator(this::compareApplicationInfo),
        addFilter = this::canAddToSet,
        stringMapper = this::fromPackageName,
        objectMapper = ApplicationInfo::packageName
    )

    val autoRotatePreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = WATCHES_WINDOW_CONTENT,
        default = false
    )

    val applicationInfoComparator: Comparator<ApplicationInfo>
        get() = Comparator { infoA, infoB -> this.compareApplicationInfo(infoA, infoB) }

    val lastSeenApps
        get() = shifter.flowable.map { it.mapNotNull(this::fromPackageName) }
            .subscribeOn(Schedulers.io())

    val unRemovablePackages = listOfNotNull(
        ANDROID_SYSTEM_UI_PACKAGE,
        App.instance?.packageName
    )

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

    init {
        setManager.addToSet(Preference.NonRotatingApps, ANDROID_SYSTEM_UI_PACKAGE)
        App.withApp { app -> setManager.addToSet(Preference.NonRotatingApps, app.packageName) }
    }

    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        isAutoRotateOn = !isAutoRotateOn
    }

    override fun accepts(gesture: GestureAction): Boolean = gesture == GestureAction.TOGGLE_AUTO_ROTATE

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!App.canWriteToSettings || event.eventType != TYPE_WINDOW_STATE_CHANGED) return

        val sequence = event.packageName ?: return

        val packageName = sequence.toString()
        if (packageName == lastPackageName) return

        val rotationApps = setManager.getSet(Preference.RotatingApps)
        if (rotationApps.isEmpty() || setManager.getSet(Preference.NonRotatingApps).contains(packageName))
            return

        lastPackageName = packageName
        shifter + packageName
        isAutoRotateOn = rotationApps.contains(packageName)
    }

    fun getAddText(preferencesName: Preference): String = App.transformApp({ app ->
        app.getString(R.string.auto_rotate_add, if (Preference.RotatingApps == preferencesName)
            app.getString(R.string.auto_rotate_apps)
        else
            app.getString(R.string.auto_rotate_apps_excluded))
    }, EMPTY_STRING)

    fun getRemoveText(preferencesName: Preference): String = App.transformApp({ app ->
        app.getString(R.string.auto_rotate_remove, if (Preference.RotatingApps == preferencesName)
            app.getString(R.string.auto_rotate_apps)
        else
            app.getString(R.string.auto_rotate_apps_excluded))
    }, EMPTY_STRING)

    fun enableWindowContentWatching(enabled: Boolean) {
        App.withApp { app ->
            app.preferences.edit().putBoolean(WATCHES_WINDOW_CONTENT, enabled).apply()

            val intent = Intent(ACTION_WATCH_WINDOW_CHANGES)
            intent.putExtra(EXTRA_WATCHES_WINDOWS, enabled)

            app.broadcast(intent)
        }
    }

    private fun canAddToSet(preferenceName: Preference): Boolean {
        val set = setManager.getSet(preferenceName)
        val count = set.filterNot(unRemovablePackages::contains).count()
        return count < 2 || PurchasesManager.instance.isPremiumNotTrial
    }

    private fun compareApplicationInfo(infoA: ApplicationInfo, infoB: ApplicationInfo): Int {
        return App.transformApp({ app ->
            val packageManager = app.packageManager
            packageManager.getApplicationLabel(infoA).toString().compareTo(packageManager.getApplicationLabel(infoB).toString())
        }, 0)
    }

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