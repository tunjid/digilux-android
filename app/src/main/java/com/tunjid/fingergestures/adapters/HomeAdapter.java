package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.HomeViewHolder;
import com.tunjid.fingergestures.viewholders.MapperViewHolder;
import com.tunjid.fingergestures.viewholders.ScreenFilterViewHolder;
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
    private static final int SCREEN_FILTER = 3;
    private static final int ADAPTIVE_BRIGHTNESS = 4;
    private static final int MAP_UP_ICON = 5;
    private static final int MAP_DOWN_ICON = 6;
    private static final int MAP_LEFT_ICON = 7;
    private static final int MAP_RIGHT_ICON = 8;
    private static final int NUM_ITEMS = 9;

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
                        (increment) -> context.getString(R.string.delta_percent, increment));
            case SLIDER_POSITION:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_position,
                        brightnessGestureConsumer.getPositionPercentage(),
                        brightnessGestureConsumer::setPositionPercentage,
                        (percentage) -> context.getString(R.string.position_percent, percentage));
            case SLIDER_COLOR:
                return new ColorAdjusterViewHolder(getView(R.layout.viewholder_slider_color, parent));
            case ADAPTIVE_BRIGHTNESS:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.adaptive_brightness,
                        brightnessGestureConsumer::restoresAdaptiveBrightnessOnDisplaySleep,
                        brightnessGestureConsumer::shouldRestoreAdaptiveBrightnessOnDisplaySleep);
            case MAP_UP_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), UP_GESTURE);
            case MAP_DOWN_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), DOWN_GESTURE);
            case MAP_LEFT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), LEFT_GESTURE);
            case MAP_RIGHT_ICON:
                return new MapperViewHolder(getView(R.layout.viewholder_mapper, parent), RIGHT_GESTURE);
            case SCREEN_FILTER:
                return new ScreenFilterViewHolder(getView(R.layout.viewholder_screen_filter, parent));
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
        boolean hasPurchasedPremium();
    }

    private View getView(@LayoutRes int res, ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
    }
}
