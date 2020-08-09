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
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent.TYPES_ALL_MASK
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.tunjid.androidx.recyclerview.diff.Diff
import com.tunjid.androidx.recyclerview.diff.Differentiable
import io.reactivex.Flowable
import io.reactivex.Flowable.timer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import java.util.concurrent.TimeUnit

class App : android.app.Application() {
    private val broadcaster = PublishProcessor.create<Intent>()

    val preferences: SharedPreferences
        get() = getSharedPreferences(BRIGHTNESS_PREFS, Context.MODE_PRIVATE)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun broadcast(intent: Intent) = broadcaster.onNext(intent)

    // Wrap the subject so if there's an error downstream, it doesn't propagate back up to it.
    // This way, the broadcast stream should never error or terminate
    fun broadcasts(): Flowable<Intent> =
            Flowable.defer { broadcaster }.onErrorResumeNext(Function<Throwable, Publisher<out Intent>> { t -> this@App.logAndResume(t) })

    // Log the error, and re-wrap the broadcast processor
    private fun logAndResume(throwable: Throwable): Flowable<Intent> {
        Log.e("App Broadcasts", "Error in broadcast stream", throwable)
        return broadcasts()
    }

    companion object {

        private const val BRIGHTNESS_PREFS = "brightness prefs"
        const val EMPTY = ""

        var instance: App? = null
            private set

        val settingsIntent: Intent
            get() =
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + instance!!.packageName))

        val accessibilityIntent: Intent
            get() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        val doNotDisturbIntent: Intent
            get() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

        fun delay(interval: Long, timeUnit: TimeUnit, runnable: () -> Unit): Disposable {
            return timer(interval, timeUnit).subscribe({ runnable.invoke() }, { it.printStackTrace() })
        }

        fun canWriteToSettings(): Boolean {
            return transformApp({ Settings.System.canWrite(it) }, false)
        }

        val hasStoragePermission: Boolean
            get() {
                return transformApp({ app -> ContextCompat.checkSelfPermission(app, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED }, false)
            }

        val isPieOrHigher: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        fun hasDoNotDisturbAccess(): Boolean {
            return transformApp({ app ->
                val notificationManager = app.getSystemService(NotificationManager::class.java)
                notificationManager != null && notificationManager.isNotificationPolicyAccessGranted
            }, false)
        }

        fun accessibilityServiceEnabled(): Boolean {
            return transformApp({ app ->
                val key = app.packageName

                val accessibilityManager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                        ?: return@transformApp false

                val list = accessibilityManager.getEnabledAccessibilityServiceList(TYPES_ALL_MASK)

                for (info in list) if (info.id.contains(key)) return@transformApp true
                false
            }, false)
        }

        fun withApp(appConsumer: (App) -> Unit) {
            val app = instance ?: return

            appConsumer.invoke(app)
        }

        fun <T> transformApp(appTFunction: (App) -> T, defaultValue: T): T {
            val app = instance
            return if (app != null) appTFunction.invoke(app) else defaultValue
        }

        fun <T> transformApp(appTFunction: (App) -> T?): T? = transformApp(appTFunction, null)

        fun <T> diff(list: MutableList<T>, supplier: () -> List<T>): Single<DiffUtil.DiffResult> =
                diff(list, supplier, { it.toString() })

        fun <T> diff(list: MutableList<T>,
                     supplier: () -> List<T>,
                     diffFunction: (T) -> String): Single<DiffUtil.DiffResult> =
                backgroundToMain {
                    Diff.calculate(
                            list,
                            supplier.invoke(),
                            { _, newList -> newList },
                            { item -> Differentiable.fromCharSequence { diffFunction.invoke(item) } })
                }
                        .doOnSuccess { diff -> list.clear(); list.addAll(diff.items) }
                        .map { diff -> diff.result }

        fun <T> backgroundToMain(supplier: () -> T): Single<T> {
            return Single.fromCallable<T> { supplier.invoke() }
                    .subscribeOn(Schedulers.io())
                    .observeOn(mainThread())
        }
    }
}
