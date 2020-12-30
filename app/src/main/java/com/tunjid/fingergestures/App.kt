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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent.TYPES_ALL_MASK
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.tunjid.fingergestures.di.Dagger
import io.reactivex.Flowable.timer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

val Context.preferences: SharedPreferences get() = getSharedPreferences(BRIGHTNESS_PREFS, Context.MODE_PRIVATE)

class App : android.app.Application() {

    val dagger: Dagger by lazy { Dagger.make(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

//    // Wrap the subject so if there's an error downstream, it doesn't propagate back up to it.
//    // This way, the broadcast stream should never error or terminate
//    fun broadcasts(): Flowable<Intent> =
//        Flowable.defer { broadcaster }.onErrorResumeNext(Function<Throwable, Publisher<out Intent>> { t -> this@App.logAndResume(t) })
//
//    // Log the error, and re-wrap the broadcast processor
//    private fun logAndResume(throwable: Throwable): Flowable<Intent> {
//        Log.e("App Broadcasts", "Error in broadcast stream", throwable)
//        return broadcasts()
//    }

    companion object {
       private var instance: App? = null

        fun delay(interval: Long, timeUnit: TimeUnit, runnable: () -> Unit): Disposable {
            return timer(interval, timeUnit).subscribe({ runnable.invoke() }, { it.printStackTrace() })
        }

        val hasStoragePermission: Boolean
            get() = instance?.hasStoragePermission ?: false

        val isPieOrHigher: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        val hasDoNotDisturbAccess: Boolean
            get() = instance?.hasDoNotDisturbAccess ?: false

        val accessibilityServiceEnabled: Boolean
            get() = instance?.accessibilityServiceEnabled ?: false

        private fun <T> transformApp(appTFunction: (App) -> T, defaultValue: T): T {
            val app = instance
            return if (app != null) appTFunction.invoke(app) else defaultValue
        }

    }
}

val Context.canWriteToSettings: Boolean
    get() = Settings.System.canWrite(this)

val Context.hasStoragePermission: Boolean
    get() =
        ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED

val Context.hasDoNotDisturbAccess: Boolean
    get() = getSystemService<NotificationManager>()
        ?.let(NotificationManager::isNotificationPolicyAccessGranted) == true

val Context.accessibilityServiceEnabled: Boolean
    get() {
        val key = packageName

        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val list = accessibilityManager.getEnabledAccessibilityServiceList(TYPES_ALL_MASK)

        for (info in list) if (info.id.contains(key)) return true
        return false
    }

private const val BRIGHTNESS_PREFS = "brightness prefs"
