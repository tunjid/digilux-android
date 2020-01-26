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

package com.tunjid.fingergestures.adapters

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.recyclerview.adapterOf
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.baseclasses.MainActivityFragment
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOWN_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.LEFT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.RIGHT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.UP_GESTURE
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.Companion.EXCLUDED_APPS
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.Companion.ROTATION_APPS
import com.tunjid.fingergestures.models.AppState
import com.tunjid.fingergestures.viewholders.*
import com.tunjid.fingergestures.viewholders.LinkViewHolder.Companion.REVIEW_LINK_ITEM
import com.tunjid.fingergestures.viewholders.LinkViewHolder.Companion.SUPPORT_LINK_ITEM
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ACCESSIBILITY_SINGLE_CLICK
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ADAPTIVE_BRIGHTNESS
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.AD_FREE
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ANIMATES_POPUP
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ANIMATES_SLIDER
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.AUDIO_DELTA
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.AUDIO_SLIDER_SHOW
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.AUDIO_STREAM_TYPE
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.DISCRETE_BRIGHTNESS
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.DOUBLE_SWIPE_SETTINGS
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ENABLE_ACCESSIBILITY_BUTTON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ENABLE_WATCH_WINDOWS
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.EXCLUDED_ROTATION_LOCK
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.LOCKED_CONTENT
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_DOWN_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_LEFT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_RIGHT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_UP_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.NAV_BAR_COLOR
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.PADDING
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.POPUP_ACTION
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.REVIEW
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.ROTATION_LOCK
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SCREEN_DIMMER
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SHOW_SLIDER
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SLIDER_COLOR
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SLIDER_DELTA
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SLIDER_DURATION
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SLIDER_POSITION
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SUPPORT
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.USE_LOGARITHMIC_SCALE
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.WALLPAPER_PICKER
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.WALLPAPER_TRIGGER


fun appAdapter(
        items: IntArray,
        state: AppState,
        listener: AppAdapterListener
): RecyclerView.Adapter<AppViewHolder> = adapterOf(
        itemsSource = { items.toList() },
        viewHolderCreator = { viewGroup, viewType ->
            val context = viewGroup.context
            val brightnessGestureConsumer = BrightnessGestureConsumer.instance
            val rotationGestureConsumer = RotationGestureConsumer.instance
            val popUpGestureConsumer = PopUpGestureConsumer.instance
            val audioGestureConsumer = AudioGestureConsumer.instance
            val backgroundManager = BackgroundManager.instance
            val purchasesManager = PurchasesManager.instance

            when (viewType) {
                PADDING -> AppViewHolder(viewGroup.inflate(R.layout.viewholder_padding), listener)

                SLIDER_DELTA -> SliderAdjusterViewHolder(
                        viewGroup.inflate(R.layout.viewholder_slider_delta),
                        R.string.adjust_slider_delta,
                        { brightnessGestureConsumer.incrementPercentage = it },
                        { brightnessGestureConsumer.incrementPercentage },
                        brightnessGestureConsumer::canAdjustDelta,
                        brightnessGestureConsumer::getAdjustDeltaText)

                SLIDER_POSITION -> SliderAdjusterViewHolder(
                        viewGroup.inflate(R.layout.viewholder_slider_delta),
                        R.string.adjust_slider_position,
                        { brightnessGestureConsumer.positionPercentage = it },
                        { brightnessGestureConsumer.positionPercentage },
                        { true },
                        { percentage -> context.getString(R.string.position_percent, percentage) })

                SLIDER_DURATION -> SliderAdjusterViewHolder(
                        viewGroup.inflate(R.layout.viewholder_slider_delta),
                        R.string.adjust_slider_duration,
                        { backgroundManager.sliderDurationPercentage = it },
                        { backgroundManager.sliderDurationPercentage },
                        { true },
                        backgroundManager::getSliderDurationText)

                DISCRETE_BRIGHTNESS -> DiscreteBrightnessViewHolder(viewGroup.inflate(R.layout.viewholder_horizontal_list), state.brightnessValues, listener)

                SLIDER_COLOR -> ColorAdjusterViewHolder(viewGroup.inflate(R.layout.viewholder_slider_color), listener)

                SCREEN_DIMMER -> ScreenDimmerViewHolder(viewGroup.inflate(R.layout.viewholder_screen_dimmer), listener)

                ADAPTIVE_BRIGHTNESS -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.adaptive_brightness,
                        brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep
                ) { flag ->
                    brightnessGestureConsumer.shouldRestoreAdaptiveBrightnessOnDisplaySleep(flag)
                    listener.notifyItemChanged(ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS)
                }

                USE_LOGARITHMIC_SCALE -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.use_logarithmic_scale,
                        brightnessGestureConsumer::usesLogarithmicScale,
                        brightnessGestureConsumer::shouldUseLogarithmicScale)

                SHOW_SLIDER -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.show_slider,
                        brightnessGestureConsumer::shouldShowSlider,
                        brightnessGestureConsumer::setSliderVisible)

                ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS -> SliderAdjusterViewHolder(
                        viewGroup.inflate(R.layout.viewholder_slider_delta),
                        R.string.adjust_adaptive_threshold,
                        R.string.adjust_adaptive_threshold_description,
                        { brightnessGestureConsumer.adaptiveBrightnessThreshold = it },
                        { brightnessGestureConsumer.adaptiveBrightnessThreshold },
                        brightnessGestureConsumer::supportsAmbientThreshold,
                        brightnessGestureConsumer::getAdaptiveBrightnessThresholdText)

                ENABLE_WATCH_WINDOWS -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.selective_app_rotation,
                        rotationGestureConsumer::canAutoRotate,
                        rotationGestureConsumer::enableWindowContentWatching)

                ENABLE_ACCESSIBILITY_BUTTON -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.popup_enable,
                        popUpGestureConsumer::hasAccessibilityButton,
                        popUpGestureConsumer::enableAccessibilityButton)

                ACCESSIBILITY_SINGLE_CLICK -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.popup_single_click,
                        { popUpGestureConsumer.isSingleClick },
                        { popUpGestureConsumer.isSingleClick = it })

                DOUBLE_SWIPE_SETTINGS -> {
                    val mapper = GestureMapper.instance
                    SliderAdjusterViewHolder(
                            viewGroup.inflate(R.layout.viewholder_slider_delta),
                            R.string.adjust_double_swipe_settings,
                            { mapper.doubleSwipeDelay = it },
                            { mapper.doubleSwipeDelay },
                            { PurchasesManager.instance.isPremium },
                            mapper::getSwipeDelayText)
                }
                ANIMATES_POPUP -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.popup_animate_in,
                        popUpGestureConsumer::shouldAnimatePopup,
                        popUpGestureConsumer::setAnimatesPopup)

                ANIMATES_SLIDER -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.slider_animate,
                        brightnessGestureConsumer::shouldAnimateSlider,
                        brightnessGestureConsumer::setAnimatesSlider)

                NAV_BAR_COLOR -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.use_colored_nav,
                        backgroundManager::usesColoredNav,
                        backgroundManager::setUsesColoredNav)

                LOCKED_CONTENT -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.set_locked_content,
                        purchasesManager::hasLockedContent,
                        purchasesManager::setHasLockedContent)

                AUDIO_DELTA -> SliderAdjusterViewHolder(
                        viewGroup.inflate(R.layout.viewholder_slider_delta),
                        R.string.audio_stream_delta,
                        0,
                        { audioGestureConsumer.volumeDelta = it },
                        { audioGestureConsumer.volumeDelta },
                        audioGestureConsumer::canSetVolumeDelta,
                        audioGestureConsumer::getChangeText)

                AUDIO_SLIDER_SHOW -> ToggleViewHolder(viewGroup.inflate(R.layout.viewholder_toggle),
                        R.string.audio_stream_slider_show,
                        audioGestureConsumer::shouldShowSliders,
                        audioGestureConsumer::setShowsSliders)

                AUDIO_STREAM_TYPE -> AudioStreamViewHolder(viewGroup.inflate(R.layout.viewholder_audio_stream_type), listener)

                MAP_UP_ICON -> MapperViewHolder(viewGroup.inflate(R.layout.viewholder_mapper), UP_GESTURE, listener)

                MAP_DOWN_ICON -> MapperViewHolder(viewGroup.inflate(R.layout.viewholder_mapper), DOWN_GESTURE, listener)

                MAP_LEFT_ICON -> MapperViewHolder(viewGroup.inflate(R.layout.viewholder_mapper), LEFT_GESTURE, listener)

                MAP_RIGHT_ICON -> MapperViewHolder(viewGroup.inflate(R.layout.viewholder_mapper), RIGHT_GESTURE, listener)

                AD_FREE -> AdFreeViewHolder(viewGroup.inflate(R.layout.viewholder_simple_text), listener)

                SUPPORT -> LinkViewHolder(viewGroup.inflate(R.layout.viewholder_simple_text), SUPPORT_LINK_ITEM, listener)

                REVIEW -> LinkViewHolder(viewGroup.inflate(R.layout.viewholder_simple_text), REVIEW_LINK_ITEM, listener)

                WALLPAPER_PICKER -> WallpaperViewHolder(viewGroup.inflate(R.layout.viewholder_wallpaper_pick), listener)

                WALLPAPER_TRIGGER -> WallpaperTriggerViewHolder(viewGroup.inflate(R.layout.viewholder_wallpaper_trigger), listener)

                ROTATION_LOCK -> RotationViewHolder(viewGroup.inflate(R.layout.viewholder_horizontal_list), ROTATION_APPS, state.rotationApps, listener)

                EXCLUDED_ROTATION_LOCK -> RotationViewHolder(viewGroup.inflate(R.layout.viewholder_horizontal_list), EXCLUDED_APPS, state.excludedRotationApps, listener)

                POPUP_ACTION -> PopupViewHolder(viewGroup.inflate(R.layout.viewholder_horizontal_list), state.popUpActions, listener)

                else -> AppViewHolder(viewGroup.inflate(R.layout.viewholder_slider_delta))
            }
        },
        viewHolderBinder = { holder, _, _ -> holder.bind() },
        viewTypeFunction = { it },
        itemIdFunction = { it.toLong() },
        onViewHolderDetached = AppViewHolder::clear,
        onViewHolderRecycleFailed = { holder -> holder.clear(); false }
)

interface AppAdapterListener {
    fun purchase(@PurchasesManager.SKU sku: String)

    fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int)

    fun requestPermission(@MainActivity.PermissionRequest permission: Int)

    fun showSnackbar(@StringRes message: Int)

    fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int)

    fun showBottomSheetFragment(fragment: MainActivityFragment)

    companion object {
        val noOpInstance
            get() = object : AppAdapterListener {
                override fun purchase(@PurchasesManager.SKU sku: String) = Unit

                override fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int) = Unit

                override fun requestPermission(@MainActivity.PermissionRequest permission: Int) = Unit

                override fun showSnackbar(@StringRes message: Int) = Unit

                override fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int) = Unit

                override fun showBottomSheetFragment(fragment: MainActivityFragment) = Unit
            }
    }
}
