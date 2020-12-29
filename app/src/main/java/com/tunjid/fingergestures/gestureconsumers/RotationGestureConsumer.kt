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


import android.content.pm.ApplicationInfo
import android.os.Parcelable
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.models.Broadcast
import com.tunjid.fingergestures.services.FingerGestureService.Companion.ANDROID_SYSTEM_UI_PACKAGE
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import java.util.*
import javax.inject.Inject

class RotationGestureConsumer @Inject constructor(
    private val app: App,
    private val broadcaster: AppBroadcaster,
    private val purchasesManager: PurchasesManager
) : GestureConsumer {

    @Parcelize
    enum class Preference(override val preferenceName: String) : SetPreference, Parcelable {
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
        default = false,
        onSet = { broadcaster(Broadcast.Service.WatchesWindows(enabled = it)) }
    )

    val applicationInfoComparator: Comparator<ApplicationInfo>
        get() = Comparator { infoA, infoB -> this.compareApplicationInfo(infoA, infoB) }

    val lastSeenApps
        get() = shifter.flowable.map { it.mapNotNull(this::fromPackageName) }
            .subscribeOn(Schedulers.io())

    val unRemovablePackages = listOf(
        ANDROID_SYSTEM_UI_PACKAGE,
        app.packageName
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
        App.withApp { app ->
            setManager.editorFor(Preference.NonRotatingApps).apply {
                plus(ApplicationInfo().apply { packageName = ANDROID_SYSTEM_UI_PACKAGE })
                plus(app.applicationInfo)
            }
        }
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

    private fun canAddToSet(preferenceName: Preference): Boolean {
        val set = setManager.getSet(preferenceName)
        val count = set.filterNot(unRemovablePackages::contains).count()
        return count < 2 || purchasesManager.isPremiumNotTrial
    }

    private fun compareApplicationInfo(infoA: ApplicationInfo, infoB: ApplicationInfo): Int {
        val packageManager = app.packageManager
        return packageManager.getApplicationLabel(infoA).toString()
            .compareTo(packageManager.getApplicationLabel(infoB).toString())
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
        private const val WATCHES_WINDOW_CONTENT = "watches window content"
        private const val EMPTY_STRING = ""
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