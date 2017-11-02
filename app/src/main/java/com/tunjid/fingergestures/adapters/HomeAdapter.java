package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.viewholders.AdFreeViewHolder;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.HomeViewHolder;
import com.tunjid.fingergestures.viewholders.MapperViewHolder;
import com.tunjid.fingergestures.viewholders.ReviewViewHolder;
import com.tunjid.fingergestures.viewholders.ScreenDimmerViewHolder;
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.ToggleViewHolder;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;


public class HomeAdapter extends BaseRecyclerViewAdapter<HomeViewHolder, HomeAdapter.HomeAdapterListener> {

    private static final int SLIDER_DELTA = 0;
    private static final int SLIDER_POSITION = 1;
    private static final int SLIDER_COLOR = 2;
    private static final int SCREEN_DIMMER = 3;
    private static final int ADAPTIVE_BRIGHTNESS = 4;
    private static final int SHOW_SLIDER = 5;
    private static final int DOUBLE_SWIPE_SETTINGS = 6;
    private static final int MAP_UP_ICON = 7;
    private static final int MAP_DOWN_ICON = 8;
    private static final int MAP_LEFT_ICON = 9;
    private static final int MAP_RIGHT_ICON = 10;
    private static final int AD_FREE = 11;
    private static final int REVIEW = 12;
    private static final int NUM_ITEMS = 13;

    public HomeAdapter(HomeAdapterListener listener) {
        super(listener);
    }

    @Override
    public HomeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        BrightnessGestureConsumer brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();

        switch (viewType) {
            case SLIDER_DELTA:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_delta,
                        brightnessGestureConsumer.getIncrementPercentage(),
                        brightnessGestureConsumer::setIncrementPercentage,
                        () -> true,
                        (increment) -> context.getString(R.string.delta_percent, increment));
            case SLIDER_POSITION:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_position,
                        brightnessGestureConsumer.getPositionPercentage(),
                        brightnessGestureConsumer::setPositionPercentage,
                        () -> true,
                        (percentage) -> context.getString(R.string.position_percent, percentage));
            case SLIDER_COLOR:
                return new ColorAdjusterViewHolder(getView(R.layout.viewholder_slider_color, parent), adapterListener);
            case SCREEN_DIMMER:
                return new ScreenDimmerViewHolder(getView(R.layout.viewholder_screen_dimmer, parent), adapterListener);
            case ADAPTIVE_BRIGHTNESS:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.adaptive_brightness,
                        brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep,
                        brightnessGestureConsumer::shouldRestoreAdaptiveBrightnessOnDisplaySleep);
            case SHOW_SLIDER:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.show_slider,
                        brightnessGestureConsumer::shouldShowSlider,
                        brightnessGestureConsumer::setSliderVisible);
            case DOUBLE_SWIPE_SETTINGS:
                GestureMapper mapper = GestureMapper.getInstance();
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_double_swipe_settings,
                        mapper.getDoubleSwipeDelay(),
                        mapper::setDoubleSwipeDelay,
                        () -> !PurchasesManager.getInstance().isNotPremium(),
                        mapper::getSwipeDelayText);
            case MAP_UP_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), UP_GESTURE);
            case MAP_DOWN_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), DOWN_GESTURE);
            case MAP_LEFT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), LEFT_GESTURE);
            case MAP_RIGHT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), RIGHT_GESTURE);
            case AD_FREE:
                return new AdFreeViewHolder(getView(R.layout.viewholder_simple_text, parent), adapterListener);
            case REVIEW:
                return new ReviewViewHolder(getView(R.layout.viewholder_simple_text, parent), adapterListener);
            default:
                return new HomeViewHolder(getView(R.layout.viewholder_slider_delta, parent));
        }
    }

    @Override
    public void onBindViewHolder(HomeViewHolder holder, int position) {
        holder.bind();
    }

    @Override
    public int getItemCount() {
        return NUM_ITEMS;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public interface HomeAdapterListener extends BaseRecyclerViewAdapter.AdapterListener {
        void purchase(String sku);
    }

    private View getView(@LayoutRes int res, ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
    }
}
