package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
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
import com.tunjid.fingergestures.viewholders.MapperViewHolder;
import com.tunjid.fingergestures.viewholders.PopupViewHolder;
import com.tunjid.fingergestures.viewholders.ReviewViewHolder;
import com.tunjid.fingergestures.viewholders.RotationViewHolder;
import com.tunjid.fingergestures.viewholders.ScreenDimmerViewHolder;
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.ToggleViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperTriggerViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperViewHolder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.EXCLUDED_APPS;
import static com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS;


public class AppAdapter extends BaseRecyclerViewAdapter<AppViewHolder, AppAdapter.AppAdapterListener> {

    public static final int PADDING = -1;
    public static final int SLIDER_DELTA = 0;
    public static final int SLIDER_POSITION = 1;
    public static final int SLIDER_DURATION = 2;
    public static final int SLIDER_COLOR = 3;
    public static final int SCREEN_DIMMER = 4;
    public static final int SHOW_SLIDER = 5;
    public static final int ADAPTIVE_BRIGHTNESS = 6;
    public static final int ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS = 7;
    public static final int DOUBLE_SWIPE_SETTINGS = 8;
    public static final int MAP_UP_ICON = 9;
    public static final int MAP_DOWN_ICON = 10;
    public static final int MAP_LEFT_ICON = 11;
    public static final int MAP_RIGHT_ICON = 12;
    public static final int AD_FREE = 13;
    public static final int REVIEW = 14;
    public static final int WALLPAPER_PICKER = 15;
    public static final int WALLPAPER_TRIGGER = 16;
    public static final int ROTATION_LOCK = 17;
    public static final int EXCLUDED_ROTATION_LOCK = 18;
    public static final int ENABLE_WATCH_WINDOWS = 19;
    public static final int POPUP_ACTION = 20;
    public static final int ENABLE_ACCESSIBILITY_BUTTON = 21;
    public static final int ANIMATES_SLIDER = 22;
    public static final int ANIMATES_POPUP = 23;
    public static final int DISCRETE_BRIGHTNESS = 24;
    public static final int AUDIO_DELTA = 25;
    public static final int AUDIO_STREAM_TYPE = 26;
    public static final int AUDIO_SLIDER_SHOW = 27;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SLIDER_COLOR, SCREEN_DIMMER,
            SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS,
            DOUBLE_SWIPE_SETTINGS, MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON,
            AD_FREE, REVIEW, WALLPAPER_PICKER, WALLPAPER_TRIGGER, ROTATION_LOCK,
            EXCLUDED_ROTATION_LOCK, ENABLE_WATCH_WINDOWS, POPUP_ACTION, ENABLE_ACCESSIBILITY_BUTTON,
            ANIMATES_SLIDER, ANIMATES_POPUP, DISCRETE_BRIGHTNESS, AUDIO_DELTA, AUDIO_STREAM_TYPE,
            AUDIO_SLIDER_SHOW})
    public @interface AdapterIndex {}

    private final int[] items;

    public AppAdapter(int[] items, AppAdapterListener listener) {
        super(listener);
        setHasStableIds(true);
        this.items = items;
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        RotationGestureConsumer rotationGestureConsumer = RotationGestureConsumer.getInstance();
        PopUpGestureConsumer popUpGestureConsumer = PopUpGestureConsumer.getInstance();
        AudioGestureConsumer audioGestureConsumer = AudioGestureConsumer.getInstance();
        BackgroundManager backgroundManager = BackgroundManager.getInstance();

        switch (viewType) {
            case PADDING:
                return new AppViewHolder(getView(R.layout.viewholder_padding, parent));
            case SLIDER_DELTA:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_delta,
                        brightnessGestureConsumer::setIncrementPercentage,
                        brightnessGestureConsumer::getIncrementPercentage,
                        brightnessGestureConsumer::noDiscreteBrightness,
                        (increment) -> context.getString(R.string.delta_percent, increment));
            case SLIDER_POSITION:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_position,
                        brightnessGestureConsumer::setPositionPercentage,
                        brightnessGestureConsumer::getPositionPercentage,
                        () -> true,
                        (percentage) -> context.getString(R.string.position_percent, percentage));
            case SLIDER_DURATION:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_duration,
                        backgroundManager::setSliderDurationPercentage,
                        backgroundManager::getSliderDurationPercentage,
                        () -> true,
                        backgroundManager::getSliderDurationText);
            case DISCRETE_BRIGHTNESS:
                return new DiscreteBrightnessViewHolder(getView(R.layout.viewholder_horizontal_list, parent), adapterListener);
            case SLIDER_COLOR:
                return new ColorAdjusterViewHolder(getView(R.layout.viewholder_slider_color, parent), adapterListener);
            case SCREEN_DIMMER:
                return new ScreenDimmerViewHolder(getView(R.layout.viewholder_screen_dimmer, parent), adapterListener);
            case ADAPTIVE_BRIGHTNESS:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.adaptive_brightness,
                        brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep,
                        (flag) -> {
                            brightnessGestureConsumer.shouldRestoreAdaptiveBrightnessOnDisplaySleep(flag);
                            adapterListener.notifyItemChanged(ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS);
                        });
            case SHOW_SLIDER:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.show_slider,
                        brightnessGestureConsumer::shouldShowSlider,
                        brightnessGestureConsumer::setSliderVisible);
            case ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_adaptive_threshold,
                        R.string.adjust_adaptive_threshold_description,
                        brightnessGestureConsumer::setAdaptiveBrightnessThreshold,
                        brightnessGestureConsumer::getAdaptiveBrightnessThreshold,
                        brightnessGestureConsumer::supportsAmbientThreshold,
                        brightnessGestureConsumer::getAdaptiveBrightnessThresholdText);
            case ENABLE_WATCH_WINDOWS:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.auto_rotate_apps,
                        rotationGestureConsumer::canAutoRotate,
                        rotationGestureConsumer::enableWindowContentWatching);
            case ENABLE_ACCESSIBILITY_BUTTON:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.popup_enable,
                        popUpGestureConsumer::hasAccessibilityButton,
                        popUpGestureConsumer::enableAccessibilityButton);
            case DOUBLE_SWIPE_SETTINGS:
                GestureMapper mapper = GestureMapper.getInstance();
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_double_swipe_settings,
                        mapper::setDoubleSwipeDelay,
                        mapper::getDoubleSwipeDelay,
                        () -> PurchasesManager.getInstance().isPremium(),
                        mapper::getSwipeDelayText);
            case ANIMATES_POPUP:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.popup_animate_in,
                        popUpGestureConsumer::shouldAnimatePopup,
                        popUpGestureConsumer::setAnimatesPopup);
            case ANIMATES_SLIDER:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.slider_animate,
                        brightnessGestureConsumer::shouldAnimateSlider,
                        brightnessGestureConsumer::setAnimatesSlider);
            case AUDIO_DELTA:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.audio_stream_delta,
                        0,
                        audioGestureConsumer::setVolumeDelta,
                        audioGestureConsumer::getVolumeDelta,
                        audioGestureConsumer::canSetVolumeDelta,
                        audioGestureConsumer::getChangeText);
            case AUDIO_SLIDER_SHOW:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.audio_stream_slider_show,
                        audioGestureConsumer::shouldShowSliders,
                        audioGestureConsumer::setShowsSliders);
            case AUDIO_STREAM_TYPE:
                return new AudioStreamViewHolder(getView(R.layout.viewholder_audio_stream_type, parent), adapterListener);
            case MAP_UP_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), UP_GESTURE, adapterListener);
            case MAP_DOWN_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), DOWN_GESTURE, adapterListener);
            case MAP_LEFT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), LEFT_GESTURE, adapterListener);
            case MAP_RIGHT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), RIGHT_GESTURE, adapterListener);
            case AD_FREE:
                return new AdFreeViewHolder(getView(R.layout.viewholder_simple_text, parent), adapterListener);
            case REVIEW:
                return new ReviewViewHolder(getView(R.layout.viewholder_simple_text, parent), adapterListener);
            case WALLPAPER_PICKER:
                return new WallpaperViewHolder(getView(R.layout.viewholder_wallpaper_pick, parent), adapterListener);
            case WALLPAPER_TRIGGER:
                return new WallpaperTriggerViewHolder(getView(R.layout.viewholder_wallpaper_trigger, parent), adapterListener);
            case ROTATION_LOCK:
                return new RotationViewHolder(getView(R.layout.viewholder_horizontal_list, parent), ROTATION_APPS, adapterListener);
            case EXCLUDED_ROTATION_LOCK:
                return new RotationViewHolder(getView(R.layout.viewholder_horizontal_list, parent), EXCLUDED_APPS, adapterListener);
            case POPUP_ACTION:
                return new PopupViewHolder(getView(R.layout.viewholder_horizontal_list, parent), adapterListener);
            default:
                return new AppViewHolder(getView(R.layout.viewholder_slider_delta, parent));
        }
    }

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {holder.bind();}

    @Override
    public int getItemCount() {return items.length;}

    @Override
    public int getItemViewType(int position) {return items[position];}

    @Override
    public long getItemId(int position) {
        return items[position];
    }

    public interface AppAdapterListener extends BaseRecyclerViewAdapter.AdapterListener {
        void purchase(@PurchasesManager.SKU String sku);

        void pickWallpaper(@BackgroundManager.WallpaperSelection int selection);

        void requestPermission(@MainActivity.PermissionRequest int permission);

        void showSnackbar(@StringRes int message);

        void notifyItemChanged(@AdapterIndex int index);

        void showBottomSheetFragment(MainActivityFragment fragment);
    }

    private View getView(@LayoutRes int res, ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
    }
}
