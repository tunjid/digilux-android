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

package com.tunjid.fingergestures.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.activities.MainActivity.Companion.ACCESSIBILITY_CODE
import com.tunjid.fingergestures.activities.MainActivity.Companion.DO_NOT_DISTURB_CODE
import com.tunjid.fingergestures.activities.MainActivity.Companion.SETTINGS_CODE
import com.tunjid.fingergestures.activities.MainActivity.Companion.STORAGE_CODE
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.AppState
import com.tunjid.fingergestures.models.Brightness
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.models.TextLink
import com.tunjid.fingergestures.toLiveData
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class AppViewModel(application: Application) : AndroidViewModel(application) {

    val liveState: LiveData<AppState> by lazy {
        val brightnessGestureConsumer = BrightnessGestureConsumer.instance
        val rotationGestureConsumer = RotationGestureConsumer.instance
        val popUpGestureConsumer = PopUpGestureConsumer.instance
        val gestureMapper = GestureMapper.instance

        Flowables.combineLatest(
                Flowable.just(getApplication<Application>().links),
                brightnessGestureConsumer.discreteBrightnesses.listMap(::Brightness),
                popUpGestureConsumer.popUpActions.listMap(::Action),
                Flowable.just(gestureMapper.actions.asList()).listMap(::Action),
                installedAppsProcessor.startWith(listOf<ApplicationInfo>()).listMap(::Package),
                rotationGestureConsumer.rotatingApps.listMap(::Package),
                rotationGestureConsumer.excludedRotatingApps.listMap(::Package),
                RotationGestureConsumer.instance.lastSeenApps.listMap(::Package),
                permissionsProcessor.startWith(listOf<Int>()),
                ::AppState
        ).toLiveData() }

    private val tabItems = listOf(
            R.id.action_directions to intArrayOf(
                    PADDING, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON,
                    AD_FREE, SUPPORT, REVIEW, LOCKED_CONTENT
            ),
            R.id.action_slider to intArrayOf(
                    PADDING, SLIDER_DELTA, DISCRETE_BRIGHTNESS, SCREEN_DIMMER, USE_LOGARITHMIC_SCALE,
                    SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ANIMATES_SLIDER, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
                    DOUBLE_SWIPE_SETTINGS
            ),
            R.id.action_audio to intArrayOf(
                    PADDING, AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW
            ),
            R.id.action_accessibility_popup to intArrayOf(PADDING, ENABLE_ACCESSIBILITY_BUTTON, ACCESSIBILITY_SINGLE_CLICK,
                    ANIMATES_POPUP, ENABLE_WATCH_WINDOWS, POPUP_ACTION,
                    ROTATION_LOCK, EXCLUDED_ROTATION_LOCK, ROTATION_HISTORY
            ),
            R.id.action_wallpaper to intArrayOf(
                    PADDING, SLIDER_POSITION, SLIDER_DURATION, NAV_BAR_COLOR,
                    SLIDER_COLOR, WALLPAPER_PICKER, WALLPAPER_TRIGGER
            )
    ).toMap()

    private val quips = application.resources.getStringArray(R.array.upsell_text)
    private val quipCounter = AtomicInteger(-1)

    private val shillProcessor: PublishProcessor<String> = PublishProcessor.create()
    private val permissionsProcessor: PublishProcessor<List<Int>> = PublishProcessor.create()
    private val installedAppsProcessor: PublishProcessor<List<ApplicationInfo>> = PublishProcessor.create()

    private val permissionsQueue get() = liveState.value?.permissionsQueue ?: listOf()
    private val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCleared() = disposable.clear()

    fun itemsAt(position: Int): IntArray = tabItems.toList()[position].second

    fun resourceIndex(@IdRes resource: Int): Int = tabItems.keys.indexOf(resource)

    fun resourceAt(position: Int): Int = tabItems.toList()[position].first

    fun shill(): Flowable<String> {
        disposable.clear()
        startShilling()
        return shillProcessor
    }

    fun updateApps() {
        Single.fromCallable {
            getApplication<Application>().packageManager.getInstalledApplications(0)
                    .filter(this::isUserInstalledApp)
                    .sortedWith(RotationGestureConsumer.instance.applicationInfoComparator)
        }
                .subscribeOn(Schedulers.io())
                .subscribe(installedAppsProcessor::onNext, Throwable::printStackTrace)
                .addTo(disposable)
    }

    fun shillMoar() = shillProcessor.onNext(getNextQuip())

    fun calmIt() = disposable.clear()

    fun requestPermission(@MainActivity.PermissionRequest permission: Int) {
        if (!permissionsQueue.contains(permission)) enqueuePermission(permission)
    }

    fun onPermissionClicked(consumer: (Int) -> Unit) {
        permissionsQueue.lastOrNull()?.let(consumer)
    }

    fun onPermissionChange(requestCode: Int): Int? {
        var shouldRemove = false
        val assigner = { value: Boolean -> shouldRemove = value; value }

        val result = when (requestCode) {
            STORAGE_CODE ->
                if (assigner.invoke(App.hasStoragePermission)) R.string.storage_permission_granted
                else R.string.storage_permission_denied
            SETTINGS_CODE ->
                if (assigner.invoke(App.canWriteToSettings())) R.string.settings_permission_granted
                else R.string.settings_permission_denied
            ACCESSIBILITY_CODE ->
                if (assigner.invoke(App.accessibilityServiceEnabled())) R.string.accessibility_permission_granted
                else R.string.accessibility_permission_denied
            DO_NOT_DISTURB_CODE ->
                if (assigner.invoke(App.hasDoNotDisturbAccess())) R.string.do_not_disturb_permission_granted
                else R.string.do_not_disturb_permission_denied
            else -> return null
        }

        if (shouldRemove) removePermission(requestCode)
        return result
    }

    fun onStartChangeDestination() = permissionsProcessor.onNext(listOf())

    private fun startShilling() {
        disposable.add(Flowable.interval(10, TimeUnit.SECONDS)
                .map { getNextQuip() }
                .observeOn(mainThread())
                .subscribe(shillProcessor::onNext, Throwable::printStackTrace))
    }

    private fun getNextQuip(): String {
        if (quipCounter.incrementAndGet() >= quips.size) quipCounter.set(0)
        return quips[quipCounter.get()]
    }

    private fun isUserInstalledApp(info: ApplicationInfo): Boolean =
            info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || info.flags and ApplicationInfo.FLAG_SYSTEM == 0

    private fun enqueuePermission(permission: Int) {
        permissionsProcessor.onNext(permissionsQueue + permission)
    }

    private fun removePermission(permission: Int) {
        permissionsProcessor.onNext(permissionsQueue - permission)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SLIDER_COLOR, SCREEN_DIMMER,
            SHOW_SLIDER, USE_LOGARITHMIC_SCALE, ADAPTIVE_BRIGHTNESS,
            ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS, MAP_UP_ICON, MAP_DOWN_ICON,
            MAP_LEFT_ICON, MAP_RIGHT_ICON, AD_FREE, REVIEW, WALLPAPER_PICKER,
            WALLPAPER_TRIGGER, ROTATION_LOCK, EXCLUDED_ROTATION_LOCK,
            ENABLE_WATCH_WINDOWS, POPUP_ACTION, ENABLE_ACCESSIBILITY_BUTTON,
            ACCESSIBILITY_SINGLE_CLICK, ANIMATES_SLIDER, ANIMATES_POPUP,
            DISCRETE_BRIGHTNESS, AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW,
            NAV_BAR_COLOR, LOCKED_CONTENT, SUPPORT, ROTATION_HISTORY)
    annotation class AdapterIndex

    companion object {
        const val PADDING = -1
        const val SLIDER_DELTA = PADDING + 1
        const val SLIDER_POSITION = SLIDER_DELTA + 1
        const val SLIDER_DURATION = SLIDER_POSITION + 1
        const val SLIDER_COLOR = SLIDER_DURATION + 1
        const val SCREEN_DIMMER = SLIDER_COLOR + 1
        const val USE_LOGARITHMIC_SCALE = SCREEN_DIMMER + 1
        const val SHOW_SLIDER = USE_LOGARITHMIC_SCALE + 1
        const val ADAPTIVE_BRIGHTNESS = SHOW_SLIDER + 1
        const val ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS = ADAPTIVE_BRIGHTNESS + 1
        const val DOUBLE_SWIPE_SETTINGS = ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS + 1
        const val MAP_UP_ICON = DOUBLE_SWIPE_SETTINGS + 1
        const val MAP_DOWN_ICON = MAP_UP_ICON + 1
        const val MAP_LEFT_ICON = MAP_DOWN_ICON + 1
        const val MAP_RIGHT_ICON = MAP_LEFT_ICON + 1
        const val AD_FREE = MAP_RIGHT_ICON + 1
        const val REVIEW = AD_FREE + 1
        const val WALLPAPER_PICKER = REVIEW + 1
        const val WALLPAPER_TRIGGER = WALLPAPER_PICKER + 1
        const val ROTATION_LOCK = WALLPAPER_TRIGGER + 1
        const val EXCLUDED_ROTATION_LOCK = ROTATION_LOCK + 1
        const val ENABLE_WATCH_WINDOWS = EXCLUDED_ROTATION_LOCK + 1
        const val POPUP_ACTION = ENABLE_WATCH_WINDOWS + 1
        const val ENABLE_ACCESSIBILITY_BUTTON = POPUP_ACTION + 1
        const val ACCESSIBILITY_SINGLE_CLICK = ENABLE_ACCESSIBILITY_BUTTON + 1
        const val ANIMATES_SLIDER = ACCESSIBILITY_SINGLE_CLICK + 1
        const val ANIMATES_POPUP = ANIMATES_SLIDER + 1
        const val DISCRETE_BRIGHTNESS = ANIMATES_POPUP + 1
        const val AUDIO_DELTA = DISCRETE_BRIGHTNESS + 1
        const val AUDIO_STREAM_TYPE = AUDIO_DELTA + 1
        const val AUDIO_SLIDER_SHOW = AUDIO_STREAM_TYPE + 1
        const val NAV_BAR_COLOR = AUDIO_SLIDER_SHOW + 1
        const val LOCKED_CONTENT = NAV_BAR_COLOR + 1
        const val SUPPORT = LOCKED_CONTENT + 1
        const val ROTATION_HISTORY = SUPPORT + 1

        internal const val RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava"
        internal const val COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker"
        internal const val ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap"
        internal const val GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons"
        internal const val IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper"
        internal const val MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/"
    }
}

val Context.links
    get() = listOf(
            TextLink(getString(R.string.get_set_icon), AppViewModel.GET_SET_ICON_LINK),
            TextLink(getString(R.string.rxjava), AppViewModel.RX_JAVA_LINK),
            TextLink(getString(R.string.color_picker), AppViewModel.COLOR_PICKER_LINK),
            TextLink(getString(R.string.image_cropper), AppViewModel.IMAGE_CROPPER_LINK),
            TextLink(getString(R.string.material_design_icons), AppViewModel.MATERIAL_DESIGN_ICONS_LINK),
            TextLink(getString(R.string.android_bootstrap), AppViewModel.ANDROID_BOOTSTRAP_LINK)
    )

fun <T, R> Flowable<List<T>>.listMap(mapper: (T) -> R): Flowable<List<R>> = map { it.map(mapper) }