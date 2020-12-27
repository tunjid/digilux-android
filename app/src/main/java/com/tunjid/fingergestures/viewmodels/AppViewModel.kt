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
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.*
import com.tunjid.fingergestures.models.Shilling.Quip
import com.tunjid.fingergestures.services.FingerGestureService
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AppViewModel(application: Application) : AndroidViewModel(application), Inputs {

    val liveState: LiveData<AppState> by lazy {
        val popUpGestureConsumer = PopUpGestureConsumer.instance
        val gestureMapper = GestureMapper.instance
        val purchasesManager = PurchasesManager.instance

        Flowables.combineLatest(
            purchasesManager.state,
            Flowable.just(getApplication<Application>().links),
            popUpGestureConsumer.popUpActions.listMap(::Action),
            Flowable.just(gestureMapper.actions.asList()).listMap(::Action),
            installedAppsProcessor.startWith(listOf<ApplicationInfo>()).listMap(::Package),
            getApplication<App>().broadcasts().filter(::intentMatches).startWith(Intent()),
            inputProcessor.filterIsInstance<Input.UiInteraction>().startWith(Input.UiInteraction.Default),
            inputProcessor.filterIsInstance<Input.Permission>().permissionState,
            items,
            ::AppState
        )
            .toLiveData()
    }

    val shill: LiveData<Shilling> by lazy {
        PurchasesManager.instance.state
            .map(PurchasesManager.State::hasAds)
            .distinctUntilChanged()
            .switchMap {
                if (it) Flowable.merge(
                    shillProcessor.map(::Quip),
                    Flowable.interval(10, TimeUnit.SECONDS).map { Quip(getNextQuip()) }
                )
                else Flowable.just(Shilling.Calm)
            }
            .toLiveData()
    }

    private val quips = application.resources.getStringArray(R.array.upsell_text)
    private val quipCounter = AtomicInteger(-1)

    private val shillProcessor: PublishProcessor<String> = PublishProcessor.create()
    private val inputProcessor: PublishProcessor<Input> = PublishProcessor.create()
    private val installedAppsProcessor: PublishProcessor<List<ApplicationInfo>> = PublishProcessor.create()

    private val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCleared() = disposable.clear()

    override fun accept(input: Input): Unit = inputProcessor.onNext(input)

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

    private fun getNextQuip(): String {
        if (quipCounter.incrementAndGet() >= quips.size) quipCounter.set(0)
        return quips[quipCounter.get()]
    }

    private fun isUserInstalledApp(info: ApplicationInfo): Boolean =
        info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || info.flags and ApplicationInfo.FLAG_SYSTEM == 0

    private fun intentMatches(intent: Intent): Boolean {
        val action = intent.action
        return (BackgroundManager.ACTION_EDIT_WALLPAPER == action
            || FingerGestureService.ACTION_SHOW_SNACK_BAR == action
            || BackgroundManager.ACTION_NAV_BAR_CHANGED == action
            || PurchasesManager.ACTION_LOCKED_CONTENT_CHANGED == action)
    }

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

private val Flowable<Input.Permission>.permissionState
    get() = filterIsInstance<Input.Permission>()
        .scan(PermissionState()) { state, permission ->
            when (permission) {
                Input.Permission.Request.Storage,
                Input.Permission.Request.Settings,
                Input.Permission.Request.Accessibility,
                Input.Permission.Request.DoNotDisturb -> {
                    if (permission is Input.Permission.Request) {
                        val queue = if (state.queue.contains(permission)) state.queue - permission else state.queue
                        state.copy(queue = queue + permission)
                    } else state
                }
                is Input.Permission.Action.Clear -> state.copy(
                    queue = listOf()
                )
                is Input.Permission.Action.Clicked -> state.copy(
                    active = state.queue.lastOrNull()?.let(::Unique),
                    queue = state.queue.dropLast(1)
                )
                is Input.Permission.Action.Changed -> {
                    val (prompt, shouldRemove) = when (permission.request) {
                        Input.Permission.Request.Storage ->
                            if (App.hasStoragePermission) R.string.storage_permission_granted to true
                            else R.string.storage_permission_denied to false
                        Input.Permission.Request.Settings ->
                            if (App.canWriteToSettings) R.string.settings_permission_granted to true
                            else R.string.settings_permission_denied to false
                        Input.Permission.Request.Accessibility ->
                            if (App.accessibilityServiceEnabled) R.string.accessibility_permission_granted to true
                            else R.string.accessibility_permission_denied to false
                        Input.Permission.Request.DoNotDisturb ->
                            if (App.hasDoNotDisturbAccess) R.string.do_not_disturb_permission_granted to true
                            else R.string.do_not_disturb_permission_denied to false
                    }

                    state.copy(
                        prompt = Unique(prompt),
                        queue = when {
                            shouldRemove -> state.queue - permission.request
                            else -> state.queue
                        }
                    )
                }
            }
        }

fun <T, R> Flowable<List<T>>.listMap(mapper: (T) -> R): Flowable<List<R>> = map { it.map(mapper) }

inline fun <reified T> Flowable<*>.filterIsInstance(): Flowable<T> = filter { it is T }.cast(T::class.java)
