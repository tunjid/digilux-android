package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.baseclasses.MainActivityFragment;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer;
import com.tunjid.fingergestures.models.AppState;
import com.tunjid.fingergestures.viewholders.AdFreeViewHolder;
import com.tunjid.fingergestures.viewholders.AppViewHolder;
import com.tunjid.fingergestures.viewholders.AudioStreamViewHolder;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.DiscreteBrightnessViewHolder;
import com.tunjid.fingergestures.viewholders.LinkViewHolder;
import com.tunjid.fingergestures.viewholders.MapperViewHolder;
import com.tunjid.fingergestures.viewholders.PopupViewHolder;
import com.tunjid.fingergestures.viewholders.RotationViewHolder;
import com.tunjid.fingergestures.viewholders.ScreenDimmerViewHolder;
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.ToggleViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperTriggerViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperViewHolder;
import com.tunjid.fingergestures.viewmodels.AppViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.EXCLUDED_APPS;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;
import static com.tunjid.fingergestures.viewholders.LinkViewHolder.REVIEW_LINK_ITEM;
import static com.tunjid.fingergestures.viewholders.LinkViewHolder.SUPPORT_LINK_ITEM;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ACCESSIBILITY_SINGLE_CLICK;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ADAPTIVE_BRIGHTNESS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.AD_FREE;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ANIMATES_POPUP;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ANIMATES_SLIDER;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.AUDIO_DELTA;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.AUDIO_SLIDER_SHOW;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.AUDIO_STREAM_TYPE;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.DISCRETE_BRIGHTNESS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.DOUBLE_SWIPE_SETTINGS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ENABLE_ACCESSIBILITY_BUTTON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ENABLE_WATCH_WINDOWS;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.EXCLUDED_ROTATION_LOCK;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.LOCKED_CONTENT;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_DOWN_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_LEFT_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_RIGHT_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_UP_ICON;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.NAV_BAR_COLOR;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.PADDING;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.POPUP_ACTION;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.REVIEW;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.ROTATION_LOCK;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SCREEN_DIMMER;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SHOW_SLIDER;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_COLOR;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_DELTA;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_DURATION;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_POSITION;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.SUPPORT;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.USE_LOGARITHMIC_SCALE;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.WALLPAPER_PICKER;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.WALLPAPER_TRIGGER;


public class AppAdapter extends InteractiveAdapter<AppViewHolder, AppAdapter.AppAdapterListener> {

    private final int[] items;
    private final AppState state;

    public AppAdapter(int[] items, AppState state, AppAdapterListener listener) {
        super(listener);
        setHasStableIds(true);
        this.items = items;
        this.state = state;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        RotationGestureConsumer rotationGestureConsumer = RotationGestureConsumer.getInstance();
        PopUpGestureConsumer popUpGestureConsumer = PopUpGestureConsumer.getInstance();
        AudioGestureConsumer audioGestureConsumer = AudioGestureConsumer.getInstance();
        BackgroundManager backgroundManager = BackgroundManager.getInstance();
        PurchasesManager purchasesManager = PurchasesManager.getInstance();

        switch (viewType) {
            case PADDING:
                return new AppViewHolder(getItemView(R.layout.viewholder_padding, parent));
            case SLIDER_DELTA:
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_delta,
                        brightnessGestureConsumer::setIncrementPercentage,
                        brightnessGestureConsumer::getIncrementPercentage,
                        brightnessGestureConsumer::canAdjustDelta,
                        brightnessGestureConsumer::getAdjustDeltaText);
            case SLIDER_POSITION:
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_position,
                        brightnessGestureConsumer::setPositionPercentage,
                        brightnessGestureConsumer::getPositionPercentage,
                        () -> true,
                        (percentage) -> context.getString(R.string.position_percent, percentage));
            case SLIDER_DURATION:
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_duration,
                        backgroundManager::setSliderDurationPercentage,
                        backgroundManager::getSliderDurationPercentage,
                        () -> true,
                        backgroundManager::getSliderDurationText);
            case DISCRETE_BRIGHTNESS:
                return new DiscreteBrightnessViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), state.brightnessValues, adapterListener);
            case SLIDER_COLOR:
                return new ColorAdjusterViewHolder(getItemView(R.layout.viewholder_slider_color, parent), adapterListener);
            case SCREEN_DIMMER:
                return new ScreenDimmerViewHolder(getItemView(R.layout.viewholder_screen_dimmer, parent), adapterListener);
            case ADAPTIVE_BRIGHTNESS:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.adaptive_brightness,
                        brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep,
                        (flag) -> {
                            brightnessGestureConsumer.shouldRestoreAdaptiveBrightnessOnDisplaySleep(flag);
                            adapterListener.notifyItemChanged(ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS);
                        });
            case USE_LOGARITHMIC_SCALE:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.use_logarithmic_scale,
                        brightnessGestureConsumer::usesLogarithmicScale,
                        brightnessGestureConsumer::shouldUseLogarithmicScale);
            case SHOW_SLIDER:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.show_slider,
                        brightnessGestureConsumer::shouldShowSlider,
                        brightnessGestureConsumer::setSliderVisible);
            case ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS:
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_adaptive_threshold,
                        R.string.adjust_adaptive_threshold_description,
                        brightnessGestureConsumer::setAdaptiveBrightnessThreshold,
                        brightnessGestureConsumer::getAdaptiveBrightnessThreshold,
                        brightnessGestureConsumer::supportsAmbientThreshold,
                        brightnessGestureConsumer::getAdaptiveBrightnessThresholdText);
            case ENABLE_WATCH_WINDOWS:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.selective_app_rotation,
                        rotationGestureConsumer::canAutoRotate,
                        rotationGestureConsumer::enableWindowContentWatching);
            case ENABLE_ACCESSIBILITY_BUTTON:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.popup_enable,
                        popUpGestureConsumer::hasAccessibilityButton,
                        popUpGestureConsumer::enableAccessibilityButton);
            case ACCESSIBILITY_SINGLE_CLICK:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.popup_single_click,
                        popUpGestureConsumer::isSingleClick,
                        popUpGestureConsumer::setSingleClick);
            case DOUBLE_SWIPE_SETTINGS:
                GestureMapper mapper = GestureMapper.getInstance();
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_double_swipe_settings,
                        mapper::setDoubleSwipeDelay,
                        mapper::getDoubleSwipeDelay,
                        () -> PurchasesManager.getInstance().isPremium(),
                        mapper::getSwipeDelayText);
            case ANIMATES_POPUP:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.popup_animate_in,
                        popUpGestureConsumer::shouldAnimatePopup,
                        popUpGestureConsumer::setAnimatesPopup);
            case ANIMATES_SLIDER:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.slider_animate,
                        brightnessGestureConsumer::shouldAnimateSlider,
                        brightnessGestureConsumer::setAnimatesSlider);
            case NAV_BAR_COLOR:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.use_colored_nav,
                        backgroundManager::usesColoredNav,
                        backgroundManager::setUsesColoredNav);
            case LOCKED_CONTENT:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.set_locked_content,
                        purchasesManager::hasLockedContent,
                        purchasesManager::setHasLockedContent);
            case AUDIO_DELTA:
                return new SliderAdjusterViewHolder(
                        getItemView(R.layout.viewholder_slider_delta, parent),
                        R.string.audio_stream_delta,
                        0,
                        audioGestureConsumer::setVolumeDelta,
                        audioGestureConsumer::getVolumeDelta,
                        audioGestureConsumer::canSetVolumeDelta,
                        audioGestureConsumer::getChangeText);
            case AUDIO_SLIDER_SHOW:
                return new ToggleViewHolder(getItemView(R.layout.viewholder_toggle, parent),
                        R.string.audio_stream_slider_show,
                        audioGestureConsumer::shouldShowSliders,
                        audioGestureConsumer::setShowsSliders);
            case AUDIO_STREAM_TYPE:
                return new AudioStreamViewHolder(getItemView(R.layout.viewholder_audio_stream_type, parent), adapterListener);
            case MAP_UP_ICON:
                return new MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), UP_GESTURE, adapterListener);
            case MAP_DOWN_ICON:
                return new MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), DOWN_GESTURE, adapterListener);
            case MAP_LEFT_ICON:
                return new MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), LEFT_GESTURE, adapterListener);
            case MAP_RIGHT_ICON:
                return new MapperViewHolder(getItemView(R.layout.viewholder_mapper, parent), RIGHT_GESTURE, adapterListener);
            case AD_FREE:
                return new AdFreeViewHolder(getItemView(R.layout.viewholder_simple_text, parent), adapterListener);
            case SUPPORT:
                return new LinkViewHolder(getItemView(R.layout.viewholder_simple_text, parent), SUPPORT_LINK_ITEM, adapterListener);
            case REVIEW:
                return new LinkViewHolder(getItemView(R.layout.viewholder_simple_text, parent), REVIEW_LINK_ITEM, adapterListener);
            case WALLPAPER_PICKER:
                return new WallpaperViewHolder(getItemView(R.layout.viewholder_wallpaper_pick, parent), adapterListener);
            case WALLPAPER_TRIGGER:
                return new WallpaperTriggerViewHolder(getItemView(R.layout.viewholder_wallpaper_trigger, parent), adapterListener);
            case ROTATION_LOCK:
                return new RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), ROTATION_APPS, state.rotationApps, adapterListener);
            case EXCLUDED_ROTATION_LOCK:
                return new RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), EXCLUDED_APPS, state.excludedRotationApps, adapterListener);
            case POPUP_ACTION:
                return new PopupViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), state.popUpActions, adapterListener);
            default:
                return new AppViewHolder(getItemView(R.layout.viewholder_slider_delta, parent));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {holder.bind();}

    @Override
    public int getItemCount() {return items.length;}

    @Override
    public int getItemViewType(int position) {return items[position];}

    @Override
    public long getItemId(int position) {
        return items[position];
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull AppViewHolder holder) {
        holder.clear();
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull AppViewHolder holder) {
        holder.clear();
        return super.onFailedToRecycleView(holder);
    }

    public interface AppAdapterListener extends InteractiveAdapter.AdapterListener {
        void purchase(@PurchasesManager.SKU String sku);

        void pickWallpaper(@BackgroundManager.WallpaperSelection int selection);

        void requestPermission(@MainActivity.PermissionRequest int permission);

        void showSnackbar(@StringRes int message);

        void notifyItemChanged(@AppViewModel.AdapterIndex int index);

        void showBottomSheetFragment(MainActivityFragment fragment);
    }
}
