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

import android.content.Context
import androidx.lifecycle.LiveData
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.map
import com.tunjid.fingergestures.models.AppState
import com.tunjid.fingergestures.viewholders.LinkViewHolder

enum class Tab {
    Gestures,
    Brightness,
    Shortcuts,
    Audio,
    Display
}

private fun getTabItems() {
    listOf(
        R.id.action_directions to intArrayOf(
             AppViewModel.LOCKED_CONTENT
        ),
        R.id.action_slider to intArrayOf(
            AppViewModel.PADDING, AppViewModel.SLIDER_DELTA, AppViewModel.DISCRETE_BRIGHTNESS, AppViewModel.SCREEN_DIMMER, AppViewModel.USE_LOGARITHMIC_SCALE,
            AppViewModel.SHOW_SLIDER, AppViewModel.ADAPTIVE_BRIGHTNESS, AppViewModel.ANIMATES_SLIDER, AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
            AppViewModel.DOUBLE_SWIPE_SETTINGS
        ),
        R.id.action_audio to intArrayOf(
            AppViewModel.PADDING, AppViewModel.AUDIO_DELTA, AppViewModel.AUDIO_STREAM_TYPE, AppViewModel.AUDIO_SLIDER_SHOW
        ),
        R.id.action_accessibility_popup to intArrayOf(AppViewModel.PADDING, AppViewModel.ENABLE_ACCESSIBILITY_BUTTON, AppViewModel.ACCESSIBILITY_SINGLE_CLICK,
            AppViewModel.ANIMATES_POPUP, AppViewModel.ENABLE_WATCH_WINDOWS, AppViewModel.POPUP_ACTION,
            AppViewModel.ROTATION_LOCK, AppViewModel.EXCLUDED_ROTATION_LOCK, AppViewModel.ROTATION_HISTORY
        ),
        R.id.action_wallpaper to intArrayOf(
            AppViewModel.PADDING, AppViewModel.SLIDER_POSITION, AppViewModel.SLIDER_DURATION, AppViewModel.NAV_BAR_COLOR,
            AppViewModel.SLIDER_COLOR, AppViewModel.WALLPAPER_PICKER, AppViewModel.WALLPAPER_TRIGGER
        )
    ).toMap()
}

private fun Context.appAdapter2(
    items: IntArray,
    state: LiveData<AppState>,
    listener: AppAdapterListener
) {

    val brightnessGestureConsumer = BrightnessGestureConsumer.instance
    val rotationGestureConsumer = RotationGestureConsumer.instance
    val popUpGestureConsumer = PopUpGestureConsumer.instance
    val audioGestureConsumer = AudioGestureConsumer.instance
    val backgroundManager = BackgroundManager.instance
    val purchasesManager = PurchasesManager.instance
    val mapper = GestureMapper.instance

    listOf(
        Item.Padding(
            tab = Tab.Gestures,
            diffId = "Top"
        ),
        Item.Mapper(
            tab = Tab.Gestures,
            direction = GestureMapper.UP_GESTURE,
            listener = listener
        ),
        Item.Mapper(
            tab = Tab.Gestures,
            direction = GestureMapper.DOWN_GESTURE,
            listener = listener
        ),
        Item.Mapper(
            tab = Tab.Gestures,
            direction = GestureMapper.LEFT_GESTURE,
            listener = listener
        ),
        Item.Mapper(
            tab = Tab.Gestures,
            direction = GestureMapper.RIGHT_GESTURE,
            listener = listener
        ),
        Item.AdFree(
            tab = Tab.Gestures,
            listener = listener
        ),
        Item.Link(
            tab = Tab.Gestures,
            linkItem = LinkViewHolder.SUPPORT_LINK_ITEM,
            listener = listener
        ),
        Item.Link(
            tab = Tab.Gestures,
            linkItem = LinkViewHolder.REVIEW_LINK_ITEM,
            listener = listener
        ),
        Item.Toggle(
            tab = Tab.Gestures,
            titleRes = R.string.set_locked_content,
            supplier = purchasesManager::hasLockedContent,
            consumer = purchasesManager::setHasLockedContent
        )
    )

    items.map { itemType ->
        when (itemType) {
            AppViewModel.PADDING -> Item.Padding(AppViewModel.PADDING)

            AppViewModel.SLIDER_DELTA -> Item.Slider(
                titleRes = R.string.adjust_slider_delta,
                infoRes = 0,
                consumer = { brightnessGestureConsumer.incrementPercentage = it },
                valueSupplier = { brightnessGestureConsumer.incrementPercentage },
                enabledSupplier = brightnessGestureConsumer::canAdjustDelta,
                function = brightnessGestureConsumer::getAdjustDeltaText
            )
            AppViewModel.SLIDER_POSITION -> Item.Slider(
                titleRes = R.string.adjust_slider_position,
                infoRes = 0,
                consumer = { brightnessGestureConsumer.positionPercentage = it },
                valueSupplier = { brightnessGestureConsumer.positionPercentage },
                enabledSupplier = { true },
                function = { percentage -> getString(R.string.position_percent, percentage) }
            )
            AppViewModel.SLIDER_DURATION -> Item.Slider(
                titleRes = R.string.adjust_slider_duration,
                infoRes = 0,
                consumer = { backgroundManager.sliderDurationPercentage = it },
                valueSupplier = { backgroundManager.sliderDurationPercentage },
                enabledSupplier = { true },
                function = backgroundManager::getSliderDurationText
            )
            AppViewModel.ADAPTIVE_BRIGHTNESS -> Item.Toggle(
                titleRes = R.string.adaptive_brightness,
                supplier = brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep
            ) { flag ->
                brightnessGestureConsumer.shouldRestoreAdaptiveBrightnessOnDisplaySleep(flag)
                listener.notifyItemChanged(AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS)
            }
            AppViewModel.USE_LOGARITHMIC_SCALE -> Item.Toggle(
                titleRes = R.string.use_logarithmic_scale,
                supplier = brightnessGestureConsumer::usesLogarithmicScale,
                consumer = brightnessGestureConsumer::shouldUseLogarithmicScale
            )
            AppViewModel.SHOW_SLIDER -> Item.Toggle(
                titleRes = R.string.show_slider,
                supplier = brightnessGestureConsumer::shouldShowSlider,
                consumer = brightnessGestureConsumer::setSliderVisible
            )
            AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS -> Item.Slider(
                titleRes = R.string.adjust_adaptive_threshold,
                infoRes = R.string.adjust_adaptive_threshold_description,
                consumer = { brightnessGestureConsumer.adaptiveBrightnessThreshold = it },
                valueSupplier = { brightnessGestureConsumer.adaptiveBrightnessThreshold },
                enabledSupplier = brightnessGestureConsumer::supportsAmbientThreshold,
                function = brightnessGestureConsumer::getAdaptiveBrightnessThresholdText
            )
            AppViewModel.ENABLE_WATCH_WINDOWS -> Item.Toggle(
                titleRes = R.string.selective_app_rotation,
                supplier = rotationGestureConsumer::canAutoRotate,
                consumer = rotationGestureConsumer::enableWindowContentWatching
            )
            AppViewModel.ENABLE_ACCESSIBILITY_BUTTON -> Item.Toggle(
                titleRes = R.string.popup_enable,
                supplier = popUpGestureConsumer::hasAccessibilityButton,
                consumer = popUpGestureConsumer::enableAccessibilityButton
            )
            AppViewModel.ACCESSIBILITY_SINGLE_CLICK -> Item.Toggle(
                titleRes = R.string.popup_single_click,
                supplier = { popUpGestureConsumer.isSingleClick },
                consumer = { popUpGestureConsumer.isSingleClick = it }
            )
            AppViewModel.DOUBLE_SWIPE_SETTINGS -> Item.Slider(
                titleRes = R.string.adjust_double_swipe_settings,
                infoRes = 0,
                consumer = { mapper.doubleSwipeDelay = it },
                valueSupplier = { mapper.doubleSwipeDelay },
                enabledSupplier = { PurchasesManager.instance.isPremium },
                function = mapper::getSwipeDelayText
            )
            AppViewModel.ANIMATES_POPUP -> Item.Toggle(
                titleRes = R.string.popup_animate_in,
                supplier = popUpGestureConsumer::shouldAnimatePopup,
                consumer = popUpGestureConsumer::setAnimatesPopup
            )
            AppViewModel.ANIMATES_SLIDER -> Item.Toggle(
                titleRes = R.string.slider_animate,
                supplier = brightnessGestureConsumer::shouldAnimateSlider,
                consumer = brightnessGestureConsumer::setAnimatesSlider
            )
            AppViewModel.NAV_BAR_COLOR -> Item.Toggle(
                titleRes = R.string.use_colored_nav,
                supplier = backgroundManager::usesColoredNav,
                consumer = backgroundManager::setUsesColoredNav
            )
            AppViewModel.LOCKED_CONTENT -> Item.Toggle(
                titleRes = R.string.set_locked_content,
                supplier = purchasesManager::hasLockedContent,
                consumer = purchasesManager::setHasLockedContent
            )
            AppViewModel.AUDIO_DELTA -> Item.Slider(
                titleRes = R.string.audio_stream_delta,
                infoRes = 0,
                consumer = { audioGestureConsumer.volumeDelta = it },
                valueSupplier = { audioGestureConsumer.volumeDelta },
                enabledSupplier = audioGestureConsumer::canSetVolumeDelta,
                function = audioGestureConsumer::getChangeText
            )
            AppViewModel.AUDIO_SLIDER_SHOW -> Item.Toggle(
                titleRes = R.string.audio_stream_slider_show,
                supplier = audioGestureConsumer::shouldShowSliders,
                consumer = audioGestureConsumer::setShowsSliders
            )

            AppViewModel.ROTATION_LOCK -> Item.Rotation(
                persistedSet = RotationGestureConsumer.ROTATION_APPS,
                titleRes = R.string.auto_rotate_apps,
                infoRes = R.string.auto_rotate_description,
                items = state.map(AppState::rotationApps),
                listener = listener
            )
            AppViewModel.EXCLUDED_ROTATION_LOCK -> Item.Rotation(
                persistedSet = RotationGestureConsumer.EXCLUDED_APPS,
                titleRes = R.string.auto_rotate_apps_excluded,
                infoRes = R.string.auto_rotate_ignored_description,
                items = state.map(AppState::excludedRotationApps),
                listener = listener
            )
            AppViewModel.ROTATION_HISTORY -> Item.Rotation(
                persistedSet = null,
                titleRes = R.string.app_rotation_history_title,
                infoRes = R.string.app_rotation_history_info,
                items = state.map(AppState::rotationScreenedApps),
                listener = listener
            )
            AppViewModel.SUPPORT -> Item.Link(
                linkItem = LinkViewHolder.SUPPORT_LINK_ITEM,
                listener = listener
            )
            AppViewModel.REVIEW -> Item.Link(
                linkItem = LinkViewHolder.REVIEW_LINK_ITEM,
                listener = listener
            )
            // AUDIO_STREAM_TYPE -> AudioStreamViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_audio_stream_type),
            //         listener
            // )
            // AD_FREE -> AdFreeViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_simple_text),
            //         listener
            // )
            // WALLPAPER_PICKER -> WallpaperViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_wallpaper_pick),
            //         listener
            // )
            // WALLPAPER_TRIGGER -> WallpaperTriggerViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_wallpaper_trigger),
            //         listener
            // )
            // POPUP_ACTION -> PopupViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_horizontal_list),
            //         state.map(AppState::popUpActions),
            //         listener
            // )
            // DISCRETE_BRIGHTNESS -> DiscreteBrightnessViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_horizontal_list),
            //         state.map(AppState::brightnessValues),
            //         listener
            // )
            // SLIDER_COLOR -> ColorAdjusterViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_slider_color),
            //         listener
            // )
            // SCREEN_DIMMER -> ScreenDimmerViewHolder(
            //         viewGroup.inflate(R.layout.viewholder_screen_dimmer),
            //         listener
            // )
            else -> Item.Padding(itemType)
        }
    }
}