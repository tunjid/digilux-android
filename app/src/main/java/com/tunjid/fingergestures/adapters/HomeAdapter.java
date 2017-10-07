package com.tunjid.fingergestures.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;
import com.tunjid.fingergestures.services.FingerGestureService;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.HomeViewHolder;
import com.tunjid.fingergestures.viewholders.SliderAdjusterViewHolder;
import com.tunjid.fingergestures.viewholders.ToggleViewHolder;


public class HomeAdapter extends BaseRecyclerViewAdapter<HomeViewHolder, HomeAdapter.HomeAdapterListener> {

    private static final int SLIDER_DELTA = 0;
    private static final int SLIDER_POSITION = 1;
    private static final int SLIDER_COLOR = 2;
    private static final int ENABLE_MAX_BRIGHTNESS = 3;
    private static final int NUM_ITEMS = 4;

    public HomeAdapter(HomeAdapterListener listener) {
        super(listener);
    }

    @Override
    public HomeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        switch (viewType) {
            case SLIDER_DELTA:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_delta,
                        FingerGestureService.getIncrementPercentage(),
                        FingerGestureService::setIncrementPercentage,
                        (increment) -> context.getString(R.string.delta_percent, increment));
            case SLIDER_POSITION:
                return new SliderAdjusterViewHolder(
                        getView(R.layout.viewholder_slider_delta, parent),
                        R.string.adjust_slider_position,
                        FingerGestureService.getPositionPercentage(),
                        FingerGestureService::setPositionPercentage,
                        (percentage) -> context.getString(R.string.position_percent, percentage));
            case SLIDER_COLOR:
                return new ColorAdjusterViewHolder(getView(R.layout.viewholder_slider_color, parent));
            case ENABLE_MAX_BRIGHTNESS:
                return new ToggleViewHolder(getView(R.layout.viewholder_toggle, parent),
                        R.string.enable_horizontal_swiping,
                        FingerGestureService::setHorizontalSwipeEnabled);
            default:
                return new HomeViewHolder(getView(R.layout.viewholder_slider_delta, parent));
        }
    }

    @Override
    public void onBindViewHolder(HomeViewHolder holder, int position) {

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
    }

    private View getView(@LayoutRes int res, ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(res, viewGroup, false);
    }
}
