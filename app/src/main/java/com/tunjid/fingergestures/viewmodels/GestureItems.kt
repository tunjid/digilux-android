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

import androidx.fragment.app.Fragment
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Brightness
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.viewholders.ReviewLinkItem
import com.tunjid.fingergestures.viewholders.SupportLinkItem
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

enum class Tab(val resource: Int) {
    Gestures(R.id.action_directions),
    Brightness(R.id.action_slider),
    Audio(R.id.action_audio),
    Shortcuts(R.id.action_accessibility_popup),
    Display(R.id.action_wallpaper);

    companion object {
        fun at(id: Int) = values().first { it.resource == id }
    }
}

interface UiInteraction

sealed class Input {
    sealed class Permission(val code: Int) : Input() {
        object Storage : Permission(MainActivity.STORAGE_CODE)
        object Settings : Permission(MainActivity.SETTINGS_CODE)
        object Accessibility : Permission(MainActivity.ACCESSIBILITY_CODE)
        object DoNotDisturb : Permission(MainActivity.DO_NOT_DISTURB_CODE)
    }

    data class ShowSheet(val fragment: Fragment) : Input(), UiInteraction
    data class GoPremium(val description: Int) : Input(), UiInteraction
    data class Purchase(val sku: PurchasesManager.Sku) : Input(), UiInteraction
}

interface Inputs {

    fun accept(input: Input)

    val items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            simpleGestureItems,
            BrightnessGestureConsumer.instance.items,
            BackgroundManager.instance.items,
            RotationGestureConsumer.instance.items,
            PopUpGestureConsumer.instance.items,
            AudioGestureConsumer.instance.items,
            PurchasesManager.instance.items,
            GestureMapper.instance.items,
        ) { simple, brightness, background, rotation, popUp, audio, purchase, gestures ->

            (simple + brightness + background + rotation + popUp + audio + purchase + gestures)
                .groupBy(Item::tab)
                .entries
                .map { (tab, items) ->
                    items + listOf(
                        Item.Padding(
                            index = Int.MIN_VALUE,
                            tab = tab,
                            diffId = "Top"
                        ),
                        Item.Padding(
                            index = Int.MAX_VALUE,
                            tab = tab,
                            diffId = "Bottom"
                        ))
                }
                .flatten()
                .sortedBy(Item::index)
        }

    private val simpleGestureItems: Flowable<List<Item>>
        get() = Flowable.just(listOf(
            Item.Link(
                index = AppViewModel.SUPPORT,
                tab = Tab.Gestures,
                linkItem = SupportLinkItem,
                input = this@Inputs
            ),
            Item.Link(
                index = AppViewModel.REVIEW,
                tab = Tab.Gestures,
                linkItem = ReviewLinkItem,
                input = this@Inputs
            ),
        ))

    private val GestureMapper.items: Flowable<List<Item>>
        get() = directionPreferencesFlowable.map { state ->
            listOf(
                Item.Mapper(
                    index = AppViewModel.MAP_UP_ICON,
                    tab = Tab.Gestures,
                    direction = GestureMapper.UP_GESTURE,
                    doubleDirection = GestureMapper.DOUBLE_UP_GESTURE,
                    gesturePair = state.up,
                    input = this@Inputs
                ),
                Item.Mapper(
                    index = AppViewModel.MAP_DOWN_ICON,
                    tab = Tab.Gestures,
                    direction = GestureMapper.DOWN_GESTURE,
                    doubleDirection = GestureMapper.DOUBLE_DOWN_GESTURE,
                    gesturePair = state.down,
                    input = this@Inputs
                ),
                Item.Mapper(
                    index = AppViewModel.MAP_LEFT_ICON,
                    tab = Tab.Gestures,
                    direction = GestureMapper.LEFT_GESTURE,
                    doubleDirection = GestureMapper.DOUBLE_LEFT_GESTURE,
                    gesturePair = state.left,
                    input = this@Inputs
                ),
                Item.Mapper(
                    index = AppViewModel.MAP_RIGHT_ICON,
                    tab = Tab.Gestures,
                    direction = GestureMapper.RIGHT_GESTURE,
                    doubleDirection = GestureMapper.DOUBLE_RIGHT_GESTURE,
                    gesturePair = state.right,
                    input = this@Inputs
                ),
            )
        }

    private val BrightnessGestureConsumer.items: Flowable<List<Item>>
        get() = state.map {
            listOf(
                Item.Slider(
                    tab = Tab.Brightness,
                    index = AppViewModel.SLIDER_DELTA,
                    titleRes = R.string.adjust_slider_delta,
                    infoRes = 0,
                    consumer = percentagePreference.setter,
                    value = it.increment.value,
                    isEnabled = it.increment.enabled,
                    function = ::getAdjustDeltaText
                ),
                Item.Slider(
                    tab = Tab.Display,
                    index = AppViewModel.SLIDER_POSITION,
                    titleRes = R.string.adjust_slider_position,
                    infoRes = 0,
                    consumer = positionPreference.setter,
                    value = it.position.value,
                    isEnabled = it.position.enabled,
                    function = { percentage -> App.instance!!.getString(R.string.position_percent, percentage) }
                ),
                Item.Slider(
                    tab = Tab.Brightness,
                    index = AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
                    titleRes = R.string.adjust_adaptive_threshold,
                    infoRes = R.string.adjust_adaptive_threshold_description,
                    consumer = adaptiveBrightnessThresholdPreference.setter,
                    value = it.adaptive.value,
                    isEnabled = it.adaptive.enabled,
                    function = ::getAdaptiveBrightnessThresholdText
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    index = AppViewModel.ADAPTIVE_BRIGHTNESS,
                    titleRes = R.string.adaptive_brightness,
                    isChecked = it.restoresAdaptiveBrightnessOnDisplaySleep,
                    consumer = adaptiveBrightnessPreference.setter,
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    index = AppViewModel.USE_LOGARITHMIC_SCALE,
                    titleRes = R.string.use_logarithmic_scale,
                    isChecked = it.usesLogarithmicScale,
                    consumer = logarithmicBrightnessPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    index = AppViewModel.SHOW_SLIDER,
                    titleRes = R.string.show_slider,
                    isChecked = it.shouldShowSlider,
                    consumer = showSliderPreference.setter,
                ),
                Item.Toggle(
                    tab = Tab.Brightness,
                    index = AppViewModel.ANIMATES_SLIDER,
                    titleRes = R.string.slider_animate,
                    isChecked = it.shouldAnimateSlider,
                    consumer = animateSliderPreference.setter
                ),
                Item.DiscreteBrightness(
                    tab = Tab.Brightness,
                    index = AppViewModel.DISCRETE_BRIGHTNESS,
                    editor = discreteBrightnessManager,
                    brightnesses = it.discreteBrightnesses.map(Int::toString).map(::Brightness),
                    input = this@Inputs,
                )
            )
        }

    private val BackgroundManager.items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            coloredNavPreference.monitor,
            sliderDurationPreference.monitor,
        ).map { (coloredNav, sliderDuration) ->
            listOf(
                Item.Slider(
                    tab = Tab.Display,
                    index = AppViewModel.SLIDER_DURATION,
                    titleRes = R.string.adjust_slider_duration,
                    infoRes = 0,
                    value = sliderDuration,
                    isEnabled = true,
                    consumer = sliderDurationPreference.setter,
                    function = ::getSliderDurationText
                ),
                Item.Toggle(
                    tab = Tab.Display,
                    index = AppViewModel.NAV_BAR_COLOR,
                    titleRes = R.string.use_colored_nav,
                    isChecked = coloredNav,
                    consumer = coloredNavPreference.setter
                )
            )
        }

    private val PopUpGestureConsumer.items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            setManager.itemsFlowable(PopUpGestureConsumer.Preference.SavedActions).listMap(::Action),
            accessibilityButtonSingleClickPreference.monitor,
            accessibilityButtonEnabledPreference.monitor,
            animatePopUpPreference.monitor,
        ) { savedActions, isSingleClick, accessibilityButtonEnabled, animatePopup ->
            listOf(
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ENABLE_ACCESSIBILITY_BUTTON,
                    titleRes = R.string.popup_enable,
                    isChecked = accessibilityButtonEnabled,
                    consumer = accessibilityButtonEnabledPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ACCESSIBILITY_SINGLE_CLICK,
                    titleRes = R.string.popup_single_click,
                    isChecked = isSingleClick,
                    consumer = accessibilityButtonSingleClickPreference.setter
                ),
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ANIMATES_POPUP,
                    titleRes = R.string.popup_animate_in,
                    isChecked = animatePopup,
                    consumer = animatePopUpPreference.setter
                ),
                Item.PopUp(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.POPUP_ACTION,
                    items = savedActions,
                    editor = setManager,
                    accessibilityButtonEnabled = accessibilityButtonEnabled,
                    input = this@Inputs
                )
            )
        }

    private val RotationGestureConsumer.items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            setManager.itemsFlowable(RotationGestureConsumer.Preference.RotatingApps).listMap(::Package),
            setManager.itemsFlowable(RotationGestureConsumer.Preference.NonRotatingApps).listMap(::Package),
            lastSeenApps.listMap(::Package),
            autoRotatePreference.monitor,
        ) { rotating, excluded, lastSeen, canAutoRotate ->
            listOf(
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ENABLE_WATCH_WINDOWS,
                    titleRes = R.string.selective_app_rotation,
                    isChecked = canAutoRotate,
                    consumer = autoRotatePreference.setter
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ROTATION_LOCK,
                    preference = RotationGestureConsumer.Preference.RotatingApps,
                    removeText = getRemoveText(RotationGestureConsumer.Preference.RotatingApps),
                    titleRes = R.string.auto_rotate_apps,
                    infoRes = R.string.auto_rotate_description,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = setManager,
                    items = rotating,
                    input = this@Inputs
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.EXCLUDED_ROTATION_LOCK,
                    preference = RotationGestureConsumer.Preference.NonRotatingApps,
                    removeText = getRemoveText(RotationGestureConsumer.Preference.NonRotatingApps),
                    titleRes = R.string.auto_rotate_apps_excluded,
                    infoRes = R.string.auto_rotate_ignored_description,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = setManager,
                    items = excluded,
                    input = this@Inputs
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    index = AppViewModel.ROTATION_HISTORY,
                    preference = null,
                    removeText = "",
                    titleRes = R.string.app_rotation_history_title,
                    infoRes = R.string.app_rotation_history_info,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = setManager,
                    items = lastSeen,
                    input = this@Inputs
                )
            )
        }

    private val AudioGestureConsumer.items: Flowable<List<Item>>
        get() = Flowables.combineLatest(
            sliderPreference.monitor,
            incrementPreference.monitor,
            streamTypePreference.monitor
        ) { showSlider, audioValue, streamType ->
            listOf(
                Item.Toggle(
                    tab = Tab.Audio,
                    index = AppViewModel.AUDIO_SLIDER_SHOW,
                    titleRes = R.string.audio_stream_slider_show,
                    isChecked = showSlider,
                    consumer = sliderPreference.setter,
                ),
                Item.Slider(
                    tab = Tab.Audio,
                    index = AppViewModel.AUDIO_DELTA,
                    titleRes = R.string.audio_stream_delta,
                    infoRes = 0,
                    value = audioValue,
                    // TODO : make reactive
                    isEnabled = canSetVolumeDelta,
                    consumer = incrementPreference.setter,
                    function = ::getChangeText
                ),
                Item.AudioStream(
                    tab = Tab.Audio,
                    index = AppViewModel.AUDIO_STREAM_TYPE,
                    titleFunction = ::getStreamTitle,
                    hasDoNotDisturbAccess = App.hasDoNotDisturbAccess(),
                    stream = AudioGestureConsumer.Stream
                        .values()
                        .first { it.type == streamType },
                    input = this@Inputs
                )
            )
        }

    private val PurchasesManager.items
        get() = Flowables.combineLatest(
            GestureMapper.instance.doubleSwipePreference.monitor,
            state
        ) { doubleSwipe, state ->
            listOf(
                Item.AdFree(
                    index = AppViewModel.AD_FREE,
                    tab = Tab.Gestures,
                    input = this@Inputs,
                    notAdFree = state.notAdFree,
                    notPremium = !state.isPremium,
                    hasAds = state.hasAds,
                ),
                Item.Toggle(
                    tab = Tab.Gestures,
                    index = AppViewModel.LOCKED_CONTENT,
                    titleRes = R.string.set_locked_content,
                    isChecked = state.hasLockedContent,
                    consumer = lockedContentPreference.setter
                ),
                Item.Slider(
                    tab = Tab.Brightness,
                    index = AppViewModel.DOUBLE_SWIPE_SETTINGS,
                    titleRes = R.string.adjust_double_swipe_settings,
                    infoRes = 0,
                    isEnabled = state.isPremium,
                    value = doubleSwipe,
                    consumer = GestureMapper.instance.doubleSwipePreference.setter,
                    function = GestureMapper.instance::getSwipeDelayText
                ),
            )
        }
}

//    private fun Context.appAdapter2(
//        items: IntArray,
//        state: LiveData<AppState>,
//        listener: AppAdapterListener
//    ) {
//        val purchasesManager = PurchasesManager.instance
//        val mapper = GestureMapper.instance
//
//
//
//        items.map { itemType ->
//            when (itemType) {
//                AppViewModel.PADDING -> 5
//                // WALLPAPER_PICKER -> WallpaperViewHolder(
//                //         viewGroup.inflate(R.layout.viewholder_wallpaper_pick),
//                //         listener
//                // )
//                // WALLPAPER_TRIGGER -> WallpaperTriggerViewHolder(
//                //         viewGroup.inflate(R.layout.viewholder_wallpaper_trigger),
//                //         listener
//                // )
//                // )
//                // SLIDER_COLOR -> ColorAdjusterViewHolder(
//                //         viewGroup.inflate(R.layout.viewholder_slider_color),
//                //         listener
//                // )
//                // SCREEN_DIMMER -> ScreenDimmerViewHolder(
//                //         viewGroup.inflate(R.layout.viewholder_screen_dimmer),
//                //         listener
//                // )
//                else -> 5
//            }
//        }
//    }
//}

// private fun getTabItems() {
//     listOf(
//         R.id.action_directions to intArrayOf(
//             AppViewModel.LOCKED_CONTENT
//         ),
//         R.id.action_slider to intArrayOf(
//             AppViewModel.PADDING, AppViewModel.SLIDER_DELTA, AppViewModel.DISCRETE_BRIGHTNESS, AppViewModel.SCREEN_DIMMER, AppViewModel.USE_LOGARITHMIC_SCALE,
//             AppViewModel.SHOW_SLIDER, AppViewModel.ADAPTIVE_BRIGHTNESS, AppViewModel.ANIMATES_SLIDER, AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
//             AppViewModel.DOUBLE_SWIPE_SETTINGS
//         ),
//         R.id.action_audio to intArrayOf(
//             AppViewModel.PADDING, AppViewModel.AUDIO_DELTA, AppViewModel.AUDIO_STREAM_TYPE, AppViewModel.AUDIO_SLIDER_SHOW
//         ),
//         R.id.action_accessibility_popup to intArrayOf(AppViewModel.PADDING, AppViewModel.ENABLE_ACCESSIBILITY_BUTTON, AppViewModel.ACCESSIBILITY_SINGLE_CLICK,
//             AppViewModel.ANIMATES_POPUP, AppViewModel.ENABLE_WATCH_WINDOWS, AppViewModel.POPUP_ACTION,
//             AppViewModel.ROTATION_LOCK, AppViewModel.EXCLUDED_ROTATION_LOCK, AppViewModel.ROTATION_HISTORY
//         ),
//         R.id.action_wallpaper to intArrayOf(
//             AppViewModel.PADDING, AppViewModel.SLIDER_POSITION, AppViewModel.SLIDER_DURATION, AppViewModel.NAV_BAR_COLOR,
//             AppViewModel.SLIDER_COLOR, AppViewModel.WALLPAPER_PICKER, AppViewModel.WALLPAPER_TRIGGER
//         )
//     ).toMap()
// }
