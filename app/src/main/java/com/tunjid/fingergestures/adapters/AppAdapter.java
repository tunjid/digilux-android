package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.WallpaperUtils;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.viewholders.AdFreeViewHolder;
import com.tunjid.fingergestures.viewholders.AppViewHolder;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.MapperViewHolder;
import com.tunjid.fingergestures.viewholders.ReviewViewHolder;
import com.tunjid.fingergestures.viewholders.ScreenDimmerViewHolder;
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.ToggleViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperTriggerViewHolder;
import com.tunjid.fingergestures.viewholders.WallpaperViewHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;


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


//    @Retention(RetentionPolicy.SOURCE)
//    @IntDef({SLIDER_DELTA, SLIDER_POSITION, SLIDER_DURATION, SLIDER_COLOR,
//            SCREEN_DIMMER, SHOW_SLIDER, ADAPTIVE_BRIGHTNESS, ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS, DOUBLE_SWIPE_SETTINGS,
//            MAP_UP_ICON, MAP_DOWN_ICON, MAP_LEFT_ICON, MAP_RIGHT_ICON, AD_FREE, REVIEW})
//    @interface AdapterView {}

    private final int[] items;

    public AppAdapter(int[] items, AppAdapterListener listener) {
        super(listener);
        this.items = items;
    }

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();

        switch (viewType) {
            case PADDING:
                return new AppViewHolder(getView(R.layout.viewholder_padding, parent));
            case SLIDER_DELTA:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_delta,
                        brightnessGestureConsumer::setIncrementPercentage,
                        brightnessGestureConsumer::getIncrementPercentage,
                        () -> true,
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
                        brightnessGestureConsumer::setSliderDurationPercentage,
                        brightnessGestureConsumer::getSliderDurationPercentage,
                        () -> true,
                        brightnessGestureConsumer::getSliderDurationText);
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
                            notifyItemChanged(Arrays.stream(items).boxed().collect(Collectors.toList()).indexOf(ADAPTIVE_BRIGHTNESS_THRESH_SETTINGS));
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
            case DOUBLE_SWIPE_SETTINGS:
                GestureMapper mapper = GestureMapper.getInstance();
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_double_swipe_settings,
                        mapper::setDoubleSwipeDelay,
                        mapper::getDoubleSwipeDelay,
                        () -> !PurchasesManager.getInstance().isNotPremium(),
                        mapper::getSwipeDelayText);
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

    public interface AppAdapterListener extends BaseRecyclerViewAdapter.AdapterListener {
        void purchase(String sku);

        void pickWallpaper(@WallpaperUtils.WallpaperSelection int selection);

        void requestPermission(@MainActivity.PermissionRequest int permission);
    }

    private View getView(@LayoutRes int res, ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
    }
}
