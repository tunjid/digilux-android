package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
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


public class AppAdapter extends InteractiveAdapter<AppViewHolder, AppAdapter.AppAdapterListener> {

    public static final int PADDING = -1;
    public static final int SLIDER_DELTA = PADDING + 1;
    public static final int SLIDER_POSITION = SLIDER_DELTA + 1;
    public static final int SLIDER_DURATION = SLIDER_POSITION + 1;
    public static final int SLIDER_COLOR = SLIDER_DURATION + 1;
    public static final int SCREEN_DIMMER = SLIDER_COLOR + 1;
    public static final int USE_LOGARITHMIC_SCALE = SCREEN_DIMMER + 1;
    public static final int SHOW_SLIDER = USE_LOGARITHMIC_SCALE + 1;
    public static final int ADAPTIVE_BRIGHTNESS = SHOW_SLIDER + 1;
    public static final int ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS = ADAPTIVE_BRIGHTNESS + 1;
    public static final int DOUBLE_SWIPE_SETTINGS = ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS + 1;
    public static final int MAP_UP_ICON = DOUBLE_SWIPE_SETTINGS + 1;
    public static final int MAP_DOWN_ICON = MAP_UP_ICON + 1;
    public static final int MAP_LEFT_ICON = MAP_DOWN_ICON + 1;
    public static final int MAP_RIGHT_ICON = MAP_LEFT_ICON + 1;
    public static final int AD_FREE = MAP_RIGHT_ICON + 1;
    public static final int REVIEW = AD_FREE + 1;
    public static final int WALLPAPER_PICKER = REVIEW + 1;
    public static final int WALLPAPER_TRIGGER = WALLPAPER_PICKER + 1;
    public static final int ROTATION_LOCK = WALLPAPER_TRIGGER + 1;
    public static final int EXCLUDED_ROTATION_LOCK = ROTATION_LOCK + 1;
    public static final int ENABLE_WATCH_WINDOWS = EXCLUDED_ROTATION_LOCK + 1;
    public static final int POPUP_ACTION = ENABLE_WATCH_WINDOWS + 1;
    public static final int ENABLE_ACCESSIBILITY_BUTTON = POPUP_ACTION + 1;
    public static final int ACCESSIBILITY_SINGLE_CLICK = ENABLE_ACCESSIBILITY_BUTTON + 1;
    public static final int ANIMATES_SLIDER = ACCESSIBILITY_SINGLE_CLICK + 1;
    public static final int ANIMATES_POPUP = ANIMATES_SLIDER + 1;
    public static final int DISCRETE_BRIGHTNESS = ANIMATES_POPUP + 1;
    public static final int AUDIO_DELTA = DISCRETE_BRIGHTNESS + 1;
    public static final int AUDIO_STREAM_TYPE = AUDIO_DELTA + 1;
    public static final int AUDIO_SLIDER_SHOW = AUDIO_STREAM_TYPE + 1;
    public static final int SUPPORT = AUDIO_SLIDER_SHOW + 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SLIDER_COLOR, SCREEN_DIMMER,
            SHOW_SLIDER, USE_LOGARITHMIC_SCALE, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
            DOUBLE_SWIPE_SETTINGS, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON,
            AD_FREE, REVIEW, WALLPAPER_PICKER, WALLPAPER_TRIGGER, ROTATION_LOCK,
            EXCLUDED_ROTATION_LOCK, ENABLE_WATCH_WINDOWS, POPUP_ACTION, ENABLE_ACCESSIBILITY_BUTTON,
            ACCESSIBILITY_SINGLE_CLICK, ANIMATES_SLIDER, ANIMATES_POPUP, DISCRETE_BRIGHTNESS,
            AUDIO_DELTA, AUDIO_STREAM_TYPE, AUDIO_SLIDER_SHOW, SUPPORT})
    public @interface AdapterIndex {}

    private final int[] items;

    public AppAdapter(int[] items, AppAdapterListener listener) {
        super(listener);
        setHasStableIds(true);
        this.items = items;
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
                return new DiscreteBrightnessViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), adapterListener);
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
                return new RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), ROTATION_APPS, adapterListener);
            case EXCLUDED_ROTATION_LOCK:
                return new RotationViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), EXCLUDED_APPS, adapterListener);
            case POPUP_ACTION:
                return new PopupViewHolder(getItemView(R.layout.viewholder_horizontal_list, parent), adapterListener);
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

        void notifyItemChanged(@AdapterIndex int index);

        void showBottomSheetFragment(MainActivityFragment fragment);
    }
}
