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

import android.view.ViewGroup
import androidx.annotation.StringRes
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter
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
import com.tunjid.fingergestures.viewholders.AdFreeViewHolder
import com.tunjid.fingergestures.viewholders.AppViewHolder
import com.tunjid.fingergestures.viewholders.AudioStreamViewHolder
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder
import com.tunjid.fingergestures.viewholders.DiscreteBrightnessViewHolder
import com.tunjid.fingergestures.viewholders.LinkViewHolder
import com.tunjid.fingergestures.viewholders.LinkViewHolder.Companion.REVIEW_LINK_ITEM
import com.tunjid.fingergestures.viewholders.LinkViewHolder.Companion.SUPPORT_LINK_ITEM
import com.tunjid.fingergestures.viewholders.MapperViewHolder
import com.tunjid.fingergestures.viewholders.PopupViewHolder
import com.tunjid.fingergestures.viewholders.RotationViewHolder
import com.tunjid.fingergestures.viewholders.ScreenDimmerViewHolder
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder
import com.tunjid.fingergestures.viewholders.ToggleViewHolder
import com.tunjid.fingergestures.viewholders.WallpaperTriggerViewHolder
import com.tunjid.fingergestures.viewholders.WallpaperViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.AppViewModel.*


class AppAdapter(
        private val items: IntArray,
        private val state: AppState,
        listener: AppAdapterListener
) : InteractiveAdapter<AppViewHolder, AppAdapter.AppAdapterListener>(listener) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val context = parent.context
        val brightnessGestureConsumer = BrightnessGestureConsumer.instance
        val rotationGestureConsumer = RotationGestureConsumer.instance
        val popUpGestureConsumer = PopUpGestureConsumer.instance
        val audioGestureConsumer = AudioGestureConsumer.instance
        val backgroundManager = BackgroundManager.instance
        val purchasesManager = PurchasesManager.instance

        return when (viewType) {
            PADDING -> AppViewHolder(getItemView(R.layout.viewholder_padding, parent))

            SLIDER_DELTA -> SliderAdjusterViewHolder(
                    getItemView(R.layout.viewholder_slider_delta, parent),
                    R.string.adjust_slider_delta,
                    { brightnessGestureConsumer.incrementPercentage = it },
                    { brightnessGestureConsumer.incrementPercentage },
                    brightnessGestureConsumer::canAdjustDelta,
                    brightnessGestureConsumer::getAdjustDeltaText)

            SLIDER_POSITION -> SliderAdjusterViewHolder(
                    getItemView(R.layout.viewholder_slider_delta, parent),
                    R.string.adjust_slider_position,
                    { brightnessGestureConsumer.positionPercentage = it },
                    { brightnessGestureConsumer.positionPercentage },
                    { true },
                    { percentage -> context.getString(R.string.position_percent, percentage) })

            SLIDER_DURATION -> SliderAdjusterViewHolder(
                    getItemView(R.layout.viewholder_slider_delta, parent),
                    R.string.adjust_slider_duration,
                    { backgroundManager.sliderDurationPercentage = it },
                    { backgroundManager.sliderDurationPercentage },
                    { true },
                    backgroundManager::getSliderDurationText)

            DISCRETE_BRIGHTNESS -> DiscreteBrightnessViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), state.brightnessValues, adapterListener)

            SLIDER_COLOR -> ColorAdjusterViewHolder(getItemView(R.layout.viewholder_slider_color, parent), adapterListener)

            SCREEN_DIMMER -> ScreenDimmerViewHolder(getItemView(R.layout.viewholder_screen_dimmer, parent), adapterListener)

            ADAPTIVE_BRIGHTNESS -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.adaptive_brightness,
                    brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep
            ) { flag ->
                brightnessGestureConsumer.shouldRestoreAdaptiveBrightnessOnDisplaySleep(flag)
                adapterListener.notifyItemChanged(ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS)
            }

            USE_LOGARITHMIC_SCALE -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.use_logarithmic_scale,
                    brightnessGestureConsumer::usesLogarithmicScale,
                    brightnessGestureConsumer::shouldUseLogarithmicScale)

            SHOW_SLIDER -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.show_slider,
                    brightnessGestureConsumer::shouldShowSlider,
                    brightnessGestureConsumer::setSliderVisible)

            ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS -> SliderAdjusterViewHolder(
                    getItemView(R.layout.viewholder_slider_delta, parent),
                    R.string.adjust_adaptive_threshold,
                    R.string.adjust_adaptive_threshold_description,
                    { brightnessGestureConsumer.adaptiveBrightnessThreshold = it },
                    { brightnessGestureConsumer.adaptiveBrightnessThreshold },
                    brightnessGestureConsumer::supportsAmbientThreshold,
                    brightnessGestureConsumer::getAdaptiveBrightnessThresholdText)

            ENABLE_WATCH_WINDOWS -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.selective_app_rotation,
                    rotationGestureConsumer::canAutoRotate,
                    rotationGestureConsumer::enableWindowContentWatching)

            ENABLE_ACCESSIBILITY_BUTTON -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.popup_enable,
                    popUpGestureConsumer::hasAccessibilityButton,
                    popUpGestureConsumer::enableAccessibilityButton)

            ACCESSIBILITY_SINGLE_CLICK -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.popup_single_click,
                    { popUpGestureConsumer.isSingleClick },
                    { popUpGestureConsumer.isSingleClick = it })

            DOUBLE_SWIPE_SETTINGS -> {
                val mapper = GestureMapper.instance
                SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_double_swipe_settings,
                        { mapper.doubleSwipeDelay = it },
                        { mapper.doubleSwipeDelay },
                        { PurchasesManager.instance.isPremium },
                        mapper::getSwipeDelayText)
            }
            ANIMATES_POPUP -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.popup_animate_in,
                    popUpGestureConsumer::shouldAnimatePopup,
                    popUpGestureConsumer::setAnimatesPopup)

            ANIMATES_SLIDER -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.slider_animate,
                    brightnessGestureConsumer::shouldAnimateSlider,
                    brightnessGestureConsumer::setAnimatesSlider)

            NAV_BAR_COLOR -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.use_colored_nav,
                    backgroundManager::usesColoredNav,
                    backgroundManager::setUsesColoredNav)

            LOCKED_CONTENT -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.set_locked_content,
                    purchasesManager::hasLockedContent,
                    purchasesManager::setHasLockedContent)

            AUDIO_DELTA -> SliderAdjusterViewHolder(
                    getItemView(R.layout.viewholder_slider_delta, parent),
                    R.string.audio_stream_delta,
                    0,
                    { audioGestureConsumer.volumeDelta = it },
                    { audioGestureConsumer.volumeDelta },
                    audioGestureConsumer::canSetVolumeDelta,
                    audioGestureConsumer::getChangeText)

            AUDIO_SLIDER_SHOW -> ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                    R.string.audio_stream_slider_show,
                    audioGestureConsumer::shouldShowSliders,
                    audioGestureConsumer::setShowsSliders)

            AUDIO_STREAM_TYPE -> AudioStreamViewHolder(getItemView(R.layout.viewholder_audio_stream_type, parent), adapterListener)

            MAP_UP_ICON -> MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), UP_GESTURE, adapterListener)

            MAP_DOWN_ICON -> MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), DOWN_GESTURE, adapterListener)

            MAP_LEFT_ICON -> MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), LEFT_GESTURE, adapterListener)

            MAP_RIGHT_ICON -> MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), RIGHT_GESTURE, adapterListener)

            AD_FREE -> AdFreeViewHolder(getItemView(R.layout.viewholder_simple_text, parent), adapterListener)

            SUPPORT -> LinkViewHolder(getItemView(R.layout.viewholder_simple_text, parent), SUPPORT_LINK_ITEM, adapterListener)

            REVIEW -> LinkViewHolder(getItemView(R.layout.viewholder_simple_text, parent), REVIEW_LINK_ITEM, adapterListener)

            WALLPAPER_PICKER -> WallpaperViewHolder(getItemView(R.layout.viewholder_wallpaper_pick, parent), adapterListener)

            WALLPAPER_TRIGGER -> WallpaperTriggerViewHolder(getItemView(R.layout.viewholder_wallpaper_trigger, parent), adapterListener)

            ROTATION_LOCK -> RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), ROTATION_APPS, state.rotationApps, adapterListener)

            EXCLUDED_ROTATION_LOCK -> RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), EXCLUDED_APPS, state.excludedRotationApps, adapterListener)

            POPUP_ACTION -> PopupViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), state.popUpActions, adapterListener)

            else -> AppViewHolder(getItemView(R.layout.viewholder_slider_delta, parent))
        }
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) = holder.bind()

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = items[position]

    override fun getItemId(position: Int): Long = items[position].toLong()

    override fun onViewDetachedFromWindow(holder: AppViewHolder) {
        holder.clear()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onFailedToRecycleView(holder: AppViewHolder): Boolean {
        holder.clear()
        return super.onFailedToRecycleView(holder)
    }

    interface AppAdapterListener : AdapterListener {
        fun purchase(@PurchasesManager.SKU sku: String)

        fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int)

        fun requestPermission(@MainActivity.PermissionRequest permission: Int)

        fun showSnackbar(@StringRes message: Int)

        fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int)

        fun showBottomSheetFragment(fragment: MainActivityFragment)
    }
}
