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
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Pair
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.drawable.DrawableCompat
import androidx.palette.graphics.Palette
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import io.reactivex.Single
import io.reactivex.Single.error
import io.reactivex.Single.fromCallable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.computation
import java.io.File
import java.io.FileInputStream
import java.util.*

class BackgroundManager private constructor() {

    private val wallpaperTargets: Array<String>

    var sliderColor: Int
        @ColorInt
        get() = App.transformApp({ app -> app.preferences.getInt(SLIDER_COLOR, getColor(app, R.color.colorAccent)) }, Color.WHITE)
        set(@ColorInt color) {
            App.withApp { app -> app.preferences.edit().putInt(SLIDER_COLOR, color).apply() }
        }

    var backgroundColor: Int
        @ColorInt
        get() = App.transformApp({ app -> app.preferences.getInt(BACKGROUND_COLOR, getColor(app, R.color.colorPrimary)) }, Color.LTGRAY)
        set(@ColorInt color) {
            App.withApp { app -> app.preferences.edit().putInt(BACKGROUND_COLOR, color).apply() }
        }

    var sliderDurationPercentage: Int
        @IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
        get() = App.transformApp({ app -> app.preferences.getInt(SLIDER_DURATION, DEF_SLIDER_DURATION_PERCENT) }, GestureConsumer.FIFTY_PERCENT)
        set(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
            duration) {
            App.withApp { app -> app.preferences.edit().putInt(SLIDER_DURATION, duration).apply() }
        }

    val sliderDurationMillis: Int
        get() = durationPercentageToMillis(sliderDurationPercentage)

    val screenAspectRatio: IntArray?
        get() {
            val displayMetrics = App.transformApp { app -> app.resources.displayMetrics }
            return if (displayMetrics == null) null else intArrayOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }

    val screenDimensionRatio: String
        get() {
            val dimensions = screenAspectRatio
            return if (dimensions == null) "H, 16:9" else "H," + dimensions[0] + ":" + dimensions[1]
        }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(INVALID_WALLPAPER_PICK_CODE, DAY_WALLPAPER_PICK_CODE, NIGHT_WALLPAPER_PICK_CODE)
    annotation class WallpaperSelection

    init {
        wallpaperTargets = arrayOf(
                App.transformApp({ app -> app.getString(R.string.day_wallpaper) }, App.EMPTY),
                App.transformApp({ app -> app.getString(R.string.night_wallpaper) }, App.EMPTY))
    }

    fun setUsesColoredNav(usesColoredNav: Boolean) {
        App.withApp { app ->
            app.preferences.edit().putBoolean(USES_COLORED_NAV_BAR, usesColoredNav).apply()
            app.broadcast(Intent(ACTION_NAV_BAR_CHANGED))
        }
    }

    fun getSliderDurationText(@IntRange(from = GestureConsumer.ZERO_PERCENT.toLong(), to = GestureConsumer.HUNDRED_PERCENT.toLong())
                              duration: Int): String {
        val millis = durationPercentageToMillis(duration)
        val seconds = millis / 1000f
        return App.transformApp({ app -> app.getString(R.string.duration_value, seconds) }, App.EMPTY)
    }

    fun requestWallPaperConstant(@StringRes titleRes: Int, context: Context, consumer: (Int) -> Unit) {
        AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setItems(wallpaperTargets) { _, index ->
                    consumer.invoke(
                            if (index == 0) DAY_WALLPAPER_PICK_CODE
                            else NIGHT_WALLPAPER_PICK_CODE)
                }
                .show()
    }

    fun getWallpaperFile(@WallpaperSelection selection: Int, context: Context): File {
        val app = context.applicationContext
        return File(app.filesDir, getFileName(selection))
    }

    private fun getFileName(@WallpaperSelection selection: Int): String {
        return if (selection == DAY_WALLPAPER_PICK_CODE) DAY_WALLPAPER_NAME else NIGHT_WALLPAPER_NAME
    }

    fun usesColoredNav(): Boolean {
        val higherThanPie = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
        return App.transformApp({ app -> app.preferences.getBoolean(USES_COLORED_NAV_BAR, higherThanPie) }, higherThanPie)
    }

    fun tint(@DrawableRes drawableRes: Int, color: Int): Drawable {
        val normalDrawable = App.transformApp { app -> ContextCompat.getDrawable(app, drawableRes) }
                ?: return ColorDrawable(color)

        val wrapDrawable = DrawableCompat.wrap(normalDrawable)
        DrawableCompat.setTint(wrapDrawable, color)

        return wrapDrawable
    }

    fun extractPalette(): Single<Palette> {
        if (!App.hasStoragePermission) return error(Exception(ERROR_NEED_PERMISSION))

        val wallpaperManager = App.transformApp { app -> app.getSystemService(WallpaperManager::class.java) }
                ?: return error(Exception(ERROR_NO_WALLPAPER_MANAGER))

        if (isLiveWallpaper(wallpaperManager)) {
            val swatches = getLiveWallpaperWatches(wallpaperManager)
            if (swatches.isNotEmpty())
                return fromCallable { Palette.from(swatches) }.subscribeOn(computation()).observeOn(mainThread())
        }

        val drawable = (wallpaperManager.drawable
                ?: return error(Exception(ERROR_NO_DRAWABLE_FOUND)))
                as? BitmapDrawable
                ?: return error(Exception(ERROR_NOT_A_BITMAP))

        val bitmap = drawable.bitmap
        return fromCallable { Palette.from(bitmap).generate() }.subscribeOn(computation()).observeOn(mainThread())
    }

    fun restoreWallpaperChange() {
        reInitializeWallpaperChange(DAY_WALLPAPER_PICK_CODE)
        reInitializeWallpaperChange(NIGHT_WALLPAPER_PICK_CODE)
    }

    fun setWallpaperChangeTime(@WallpaperSelection selection: Int, hourOfDay: Int, minute: Int) {
        setWallpaperChangeTime(selection, hourOfDay, minute, true)
    }

    fun cancelAutoWallpaper() {
        val alarmManager = App.transformApp { app -> app.getSystemService(AlarmManager::class.java) }
                ?: return

        val day = getWallpaperPendingIntent(DAY_WALLPAPER_PICK_CODE)
        val night = getWallpaperPendingIntent(NIGHT_WALLPAPER_PICK_CODE)
        if (day == null || night == null) return

        val dayTimePair = getDefaultTime(DAY_WALLPAPER_PICK_CODE)
        val nightTimePair = getDefaultTime(NIGHT_WALLPAPER_PICK_CODE)

        setWallpaperChangeTime(NIGHT_WALLPAPER_PICK_CODE, nightTimePair.first, nightTimePair.second, false)
        setWallpaperChangeTime(DAY_WALLPAPER_PICK_CODE, dayTimePair.first, dayTimePair.second, false)
        alarmManager.cancel(night)
        alarmManager.cancel(day)
        night.cancel()
        day.cancel()
    }

    fun getMainWallpaperCalendar(@WallpaperSelection selection: Int): Calendar {
        val timePair = getDefaultTime(selection)
        val preferences = App.transformApp { it.preferences } ?: return Calendar.getInstance()

        val hour = preferences.getInt(
                if (selection == DAY_WALLPAPER_PICK_CODE) DAY_WALLPAPER_HOUR
                else NIGHT_WALLPAPER_HOUR, timePair.first
        )
        val minute = preferences.getInt(
                if (selection == DAY_WALLPAPER_PICK_CODE) DAY_WALLPAPER_MINUTE
                else NIGHT_WALLPAPER_MINUTE, timePair.second
        )

        return calendarForTime(hour, minute)
    }

    private fun getWallpaperPendingIntent(@WallpaperSelection selection: Int): PendingIntent? {
        return App.transformApp { app -> PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(app, selection), PendingIntent.FLAG_UPDATE_CURRENT) }
    }

    private fun calendarForTime(hourOfDay: Int, minute: Int): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar
    }

    internal fun onIntentReceived(intent: Intent) {
        if (handledEditPick(intent)) return

        val selection = selectionFromIntent(intent)
        if (selection == INVALID_WALLPAPER_PICK_CODE) return

        val wallpaperFile = App.transformApp { app -> getWallpaperFile(selection, app) }
                ?: return

        val wallpaperManager = App.transformApp { app -> app.getSystemService(WallpaperManager::class.java) }
        if (wallpaperManager == null || !wallpaperFile.exists()) return

        if (!App.hasStoragePermission) return
        try {
            wallpaperManager.setStream(FileInputStream(wallpaperFile))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @WallpaperSelection
    private fun selectionFromIntent(intent: Intent): Int {
        val action = intent.action
        return if (action != null && action == ACTION_CHANGE_WALLPAPER)
            intent.getIntExtra(EXTRA_CHANGE_WALLPAPER, INVALID_WALLPAPER_PICK_CODE)
        else
            INVALID_WALLPAPER_PICK_CODE
    }

    private fun reInitializeWallpaperChange(@WallpaperSelection selection: Int) = App.withApp { app ->
        val preferences = app.preferences
        val isDay = selection == DAY_WALLPAPER_PICK_CODE
        val hasCalendar = preferences.getBoolean(if (isDay) DAY_WALLPAPER_SET else NIGHT_WALLPAPER_SET, false)
        if (!hasCalendar) return@withApp

        val calendar = getMainWallpaperCalendar(selection)
        setWallpaperChangeTime(selection, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }

    private fun setWallpaperChangeTime(@WallpaperSelection selection: Int, hourOfDay: Int, minute: Int, adding: Boolean) {
        val hour = when {
            hourOfDay > 12 && selection == DAY_WALLPAPER_PICK_CODE -> hourOfDay - 12
            hourOfDay < 12 && selection == NIGHT_WALLPAPER_PICK_CODE -> hourOfDay + 12
            else -> hourOfDay
        }

        val isDayWallpaper = selection == DAY_WALLPAPER_PICK_CODE

        App.withApp { app ->
            app.preferences.edit()
                    .putInt(if (isDayWallpaper) DAY_WALLPAPER_HOUR else NIGHT_WALLPAPER_HOUR, hour)
                    .putInt(if (isDayWallpaper) DAY_WALLPAPER_MINUTE else NIGHT_WALLPAPER_MINUTE, minute)
                    .putBoolean(if (isDayWallpaper) DAY_WALLPAPER_SET else NIGHT_WALLPAPER_SET, adding)
                    .apply()
        }

        val calendar = calendarForTime(hourOfDay, minute)

        val alarmManager = App.transformApp { app -> app.getSystemService(AlarmManager::class.java) }
                ?: return

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

    fun willChangeWallpaper(@WallpaperSelection selection: Int): Boolean {
        return App.transformApp({ app ->
            val intent = Intent(app, WallpaperBroadcastReceiver::class.java)
            intent.action = ACTION_CHANGE_WALLPAPER
            intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection)

            PendingIntent.getBroadcast(app, selection, getWallPaperChangeIntent(app, selection), PendingIntent.FLAG_NO_CREATE) != null
        }, false)
    }

    private fun getDefaultTime(@WallpaperSelection selection: Int): Pair<Int, Int> {
        return Pair(if (selection == DAY_WALLPAPER_PICK_CODE) 7 else 19, 0)
    }

    fun getWallpaperEditPendingIntent(context: Context): PendingIntent {
        val app = context.applicationContext
        val intent = Intent(app, WallpaperBroadcastReceiver::class.java)
        intent.action = ACTION_EDIT_WALLPAPER
        return PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getWallPaperChangeIntent(app: Context, @WallpaperSelection selection: Int): Intent {
        val intent = Intent(app, WallpaperBroadcastReceiver::class.java)
        intent.action = ACTION_CHANGE_WALLPAPER
        intent.putExtra(EXTRA_CHANGE_WALLPAPER, selection)

        return intent
    }

    private fun handledEditPick(intent: Intent): Boolean {
        if (ACTION_EDIT_WALLPAPER != intent.action) return false

        val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_CHOSEN_COMPONENT)
                ?: return false

        val handled = componentName.packageName == "com.google.android.apps.photos"
        if (handled) App.withApp { app -> app.broadcast(intent) }

        return handled
    }

    private fun durationPercentageToMillis(percentage: Int): Int {
        return (percentage * MAX_SLIDER_DURATION / 100f).toInt()
    }

    companion object {

        private const val MAX_SLIDER_DURATION = 5000
        private const val DEF_SLIDER_DURATION_PERCENT = 60

        private const val SLIDER_DURATION = "slider duration"
        private const val BACKGROUND_COLOR = "background color"
        private const val SLIDER_COLOR = "slider color"
        private const val USES_COLORED_NAV_BAR = "colored nav bar"

        private const val ERROR_NEED_PERMISSION = "Need permission"
        private const val ERROR_NO_WALLPAPER_MANAGER = "No Wallpaper manager"
        private const val ERROR_NO_DRAWABLE_FOUND = "No Drawable found"
        private const val ERROR_NOT_A_BITMAP = "Not a Bitmap"

        private const val DAY_WALLPAPER_NAME = "day"
        private const val DAY_WALLPAPER_SET = "day wallpaper set"
        private const val DAY_WALLPAPER_HOUR = "day wallpaper hour"
        private const val DAY_WALLPAPER_MINUTE = "day wallpaper minute"

        private const val NIGHT_WALLPAPER_NAME = "night"
        private const val NIGHT_WALLPAPER_SET = "night wallpaper set"
        private const val NIGHT_WALLPAPER_HOUR = "night wallpaper hour"
        private const val NIGHT_WALLPAPER_MINUTE = "night wallpaper minute"

        private const val EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT"
        private const val EXTRA_CHANGE_WALLPAPER = "com.tunjid.fingergestures.extra.changeWallpaper"
        private const val ACTION_CHANGE_WALLPAPER = "com.tunjid.fingergestures.action.changeWallpaper"
        const val ACTION_EDIT_WALLPAPER = "com.tunjid.fingergestures.action.editWallpaper"
        const val ACTION_NAV_BAR_CHANGED = "com.tunjid.fingergestures.action.navBarChanged"

        const val DAY_WALLPAPER_PICK_CODE = 0
        const val NIGHT_WALLPAPER_PICK_CODE = 1
        private const val INVALID_WALLPAPER_PICK_CODE = -1

        val instance: BackgroundManager by lazy { BackgroundManager() }
    }
}
