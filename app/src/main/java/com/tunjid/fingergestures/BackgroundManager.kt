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


import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.AlarmManager.INTERVAL_DAY
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Parcelable
import androidx.annotation.IntRange
import androidx.palette.graphics.Palette
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.core.delegates.intentExtras
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers.computation
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

val Context.screenAspectRatio: IntArray
    get() = with(resources.displayMetrics) {
        intArrayOf(widthPixels, heightPixels)
    }

fun Context.getWallpaperFile(selection: WallpaperSelection): File =
    File(applicationContext.filesDir, selection.fileName)

@Singleton
class BackgroundManager @Inject constructor(
    @AppContext private val context: Context,
    reactivePreferences: ReactivePreferences,
    private val broadcaster: AppBroadcaster
) {

    val backgroundColorPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "background color",
        default = context.colorAt(R.color.colorPrimary)
    )
    val sliderColorPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "slider color",
        default = context.colorAt(R.color.colorAccent)
    )
    val sliderDurationPreference: ReactivePreference<Int> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "slider duration",
        default = DEF_SLIDER_DURATION_PERCENT
    )
    val coloredNavPreference: ReactivePreference<Boolean> = ReactivePreference(
        reactivePreferences = reactivePreferences,
        key = "colored nav bar",
        default = Build.VERSION.SDK_INT > Build.VERSION_CODES.P,
    )

    val dayWallpaperStatus = WallpaperSelection.Day.wallpaperStatus(reactivePreferences)
    val nightWallpaperStatus = WallpaperSelection.Night.wallpaperStatus(reactivePreferences)

    // TODO: Improve this
    val paletteFlowable: Flowable<PaletteStatus> = Flowable.defer {
        if (!context.hasStoragePermission) return@defer Flowable.just(PaletteStatus.Unavailable("Need permission"))

        val wallpaperManager = context.getSystemService(WallpaperManager::class.java)
            ?: return@defer Flowable.just(PaletteStatus.Unavailable("No Wallpaper manager"))

        if (isLiveWallpaper(wallpaperManager)) {
            val swatches = getLiveWallpaperWatches(wallpaperManager)
            if (swatches.isNotEmpty())
                return@defer Flowable.fromCallable { Palette.from(swatches) }
                    .map(PaletteStatus::Available)
                    .subscribeOn(computation())
        }

        val drawable = (wallpaperManager.drawable
            ?: return@defer Flowable.just(PaletteStatus.Unavailable("No Drawable found")))
            as? BitmapDrawable
            ?: return@defer Flowable.just(PaletteStatus.Unavailable("Not a Bitmap"))

        val bitmap = drawable.bitmap
        Flowable.fromCallable { Palette.from(bitmap).generate() }
            .map(PaletteStatus::Available)
            .subscribeOn(computation())
    }

    val sliderDurationMillis: Int
        get() = durationPercentageToMillis(sliderDurationPreference.value)

    val screenDimensionRatio: String
        get() = context.screenAspectRatio.let { dimensions ->
            "H," + dimensions[0] + ":" + dimensions[1]
        }

    val setWallpaperChangeTime = { selection: WallpaperSelection, hourOfDay: Int, minute: Int ->
        setWallpaperChangeTime(selection, hourOfDay, minute, true)
    }

    val cancelAutoWallpaper = cancel@{
        val alarmManager = context.getSystemService(AlarmManager::class.java)
            ?: return@cancel

        val day = getWallpaperPendingIntent(WallpaperSelection.Day)
        val night = getWallpaperPendingIntent(WallpaperSelection.Night)
        if (day == null || night == null) return@cancel

        val dayTimePair = WallpaperSelection.Day.defaultTime
        val nightTimePair = WallpaperSelection.Day.defaultTime

        setWallpaperChangeTime(WallpaperSelection.Night, nightTimePair.first, nightTimePair.second, false)
        setWallpaperChangeTime(WallpaperSelection.Day, dayTimePair.first, dayTimePair.second, false)
        alarmManager.cancel(night)
        alarmManager.cancel(day)
        night.cancel()
        day.cancel()
    }

    val wallpaperEditPendingIntent: PendingIntent
        get() {
            val intent = Intent(context, WallpaperBroadcastReceiver::class.java)
            intent.action = ACTION_EDIT_WALLPAPER
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    fun getSliderDurationText(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
                              duration: Int): String {
        val millis = durationPercentageToMillis(duration)
        val seconds = millis / 1000f
        return context.getString(R.string.duration_value, seconds)
    }

    fun getWallpaperFile(selection: WallpaperSelection): File = context.getWallpaperFile(selection)

    fun usesColoredNav(): Boolean = coloredNavPreference.value

    fun restoreWallpaperChange() {
        reInitializeWallpaperChange(WallpaperSelection.Day)
        reInitializeWallpaperChange(WallpaperSelection.Night)
    }

    fun getMainWallpaperCalendar(selection: WallpaperSelection): Calendar {
        val timePair = selection.defaultTime
        val preferences = context.preferences ?: return Calendar.getInstance()

        val hour = preferences.getInt(selection.hour, timePair.first)
        val minute = preferences.getInt(selection.minute, timePair.second)

        return calendarForTime(hour to minute)
    }

    private fun getWallpaperPendingIntent(selection: WallpaperSelection): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            selection.code,
            getWallPaperChangeIntent(context, selection),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    internal fun onIntentReceived(intent: Intent) {
        if (handledEditPick(intent)) return

        val selection = selectionFromIntent(intent) ?: return
        val wallpaperFile = context.getWallpaperFile(selection)

        val wallpaperManager = context.getSystemService(WallpaperManager::class.java)
        if (wallpaperManager == null || !wallpaperFile.exists()) return

        if (!context.hasStoragePermission) return
        try {
            wallpaperManager.setStream(FileInputStream(wallpaperFile))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectionFromIntent(intent: Intent): WallpaperSelection? = when (intent.action) {
        ACTION_CHANGE_WALLPAPER -> intent.changeWallpaperSelection
        else -> null
    }

    private fun reInitializeWallpaperChange(selection: WallpaperSelection) {
        val preferences = context.preferences
        val hasCalendar = preferences.getBoolean(selection.set, false)
        if (!hasCalendar) return

        val calendar = getMainWallpaperCalendar(selection)
        setWallpaperChangeTime(selection, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private fun setWallpaperChangeTime(selection: WallpaperSelection, hourOfDay: Int, minute: Int, adding: Boolean) {
        val hour = when {
            hourOfDay > 12 && selection == WallpaperSelection.Day -> hourOfDay - 12
            hourOfDay < 12 && selection == WallpaperSelection.Night -> hourOfDay + 12
            else -> hourOfDay
        }

        context.preferences.edit()
            .putInt(selection.hour, hour)
            .putInt(selection.minute, minute)
            .putBoolean(selection.set, adding)
            .apply()

        val calendar = calendarForTime(hourOfDay to minute)

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        val alarmIntent = getWallpaperPendingIntent(selection)
        alarmManager.setRepeating(RTC_WAKEUP, calendar.timeInMillis, INTERVAL_DAY, alarmIntent)
    }

    @TargetApi(Build.VERSION_CODES.O_MR1)
    private fun getLiveWallpaperWatches(wallpaperManager: WallpaperManager): List<Palette.Swatch> {
        val colors = wallpaperManager.getWallpaperColors(FLAG_SYSTEM)
        val result = ArrayList<Palette.Swatch>()
        if (colors == null) return result

        val primary = colors.primaryColor
        val secondary = colors.secondaryColor
        val tertiary = colors.tertiaryColor

        result.add(Palette.Swatch(primary.toArgb(), 3))

        if (secondary != null) result.add(Palette.Swatch(secondary.toArgb(), 2))
        if (tertiary != null) result.add(Palette.Swatch(tertiary.toArgb(), 2))

        return result
    }

    private fun isLiveWallpaper(wallpaperManager: WallpaperManager): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && wallpaperManager.wallpaperInfo != null
    }

    fun willChangeWallpaper(selection: WallpaperSelection): Boolean {
        val intent = Intent(context, WallpaperBroadcastReceiver::class.java)
        intent.action = ACTION_CHANGE_WALLPAPER
        intent.changeWallpaperSelection = selection

        return PendingIntent.getBroadcast(context, selection.code, getWallPaperChangeIntent(context, selection), PendingIntent.FLAG_NO_CREATE) != null
    }

    private fun getWallPaperChangeIntent(app: Context, selection: WallpaperSelection): Intent {
        val intent = Intent(app, WallpaperBroadcastReceiver::class.java)
        intent.action = ACTION_CHANGE_WALLPAPER
        intent.changeWallpaperSelection = selection

        return intent
    }

    private fun handledEditPick(intent: Intent): Boolean {
        if (ACTION_EDIT_WALLPAPER != intent.action) return false

        val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_CHOSEN_COMPONENT)
            ?: return false

        val handled = componentName.packageName == "com.google.android.apps.photos"
//        if (handled) broadcaster(intent)

        return handled
    }

    private fun durationPercentageToMillis(percentage: Int): Int =
        (percentage * MAX_SLIDER_DURATION / 100f).toInt()

    companion object {
        const val ACTION_EDIT_WALLPAPER = "com.tunjid.fingergestures.action.editWallpaper"
    }
}

private const val MAX_SLIDER_DURATION = 5000
private const val DEF_SLIDER_DURATION_PERCENT = 60

private const val EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT"
private const val ACTION_CHANGE_WALLPAPER = "com.tunjid.fingergestures.action.changeWallpaper"

private var Intent.changeWallpaperSelection by intentExtras<WallpaperSelection?>()

private fun WallpaperSelection.wallpaperStatus(reactivePreferences: ReactivePreferences): Flowable<WallpaperStatus> {
    val (defaultHour, defaultMinute) = this.defaultTime
    return Flowables.combineLatest(
        ReactivePreference(reactivePreferences = reactivePreferences, key = set, default = false).monitor,
        ReactivePreference(reactivePreferences = reactivePreferences, key = hour, default = defaultHour).monitor,
        ReactivePreference(reactivePreferences = reactivePreferences, key = minute, default = defaultMinute).monitor,
    ) { willChange, hour, minute ->
        WallpaperStatus(
            selection = this,
            willChange = willChange,
            calendar = calendarForTime(hour to minute)
        )
    }
}

private val WallpaperSelection.defaultTime: Pair<Int, Int>
    get() = Pair(when (this) {
        WallpaperSelection.Day -> 7
        WallpaperSelection.Night -> 19
    }, 0)

private fun calendarForTime(hourMinutePair: Pair<Int, Int>): Calendar {
    val (hourOfDay, minute) = hourMinutePair

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return calendar
}

@Parcelize
enum class WallpaperSelection(
    val code: Int,
    val textRes: Int,
    val fileName: String,
    val set: String,
    val minute: String,
    val hour: String
) : Parcelable {
    //    Invalid(code = -1),
    Day(
        code = 0,
        textRes = R.string.day_wallpaper,
        fileName = "day",
        set = "day wallpaper set",
        minute = "day wallpaper minute",
        hour = "day wallpaper hour",
    ),
    Night(
        code = 1,
        textRes = R.string.night_wallpaper,
        fileName = "night",
        set = "night wallpaper set",
        minute = "night wallpaper minute",
        hour = "night wallpaper hour",
    );
}

data class WallpaperStatus(
    val selection: WallpaperSelection,
    val calendar: Calendar,
    val willChange: Boolean,
)

sealed class PaletteStatus {
    data class Available(val palette: Palette) : PaletteStatus()
    data class Unavailable(val reason: String) : PaletteStatus()
}
