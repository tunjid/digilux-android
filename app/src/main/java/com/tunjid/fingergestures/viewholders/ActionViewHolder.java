package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunjid.androidbootstrap.core.abstractclasses.BaseViewHolder;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;


public class ActionViewHolder extends BaseViewHolder<ActionAdapter.ActionClickListener> {

    private final boolean showsText;
    private int stringRes;
    private TextView textView;
    private ImageView imageView;

    public ActionViewHolder(boolean showsText, View itemView, ActionAdapter.ActionClickListener clickListener) {
        super(itemView, clickListener);
        this.showsText = showsText;
        textView = itemView.findViewById(R.id.text);
        imageView = itemView.findViewById(R.id.icon);

        itemView.setOnClickListener(view -> adapterListener.onActionClicked(stringRes));
    }

    public void bind(@GestureConsumer.GestureAction int action) {
        this.stringRes = GestureMapper.getInstance().resourceForAction(action);

        textView.setText(stringRes);
        textView.setVisibility(showsText ? View.VISIBLE : View.GONE);

        int iconRes = resourceToIcon(stringRes);
        int iconColor = BrightnessGestureConsumer.getInstance().getSliderColor();

        if (showsText) imageView.setImageResource(iconRes);
        else imageView.setImageDrawable(BackgroundManager.getInstance().tint(iconRes, iconColor));
    }

    private int resourceToIcon(int stringRes) {
        switch (stringRes) {
            default:
            case R.string.do_nothing:
                return R.drawable.ic_blank_24dp;

            case R.string.increase_brightness:
                return R.drawable.ic_brightness_medium_24dp;

            case R.string.reduce_brightness:
                return R.drawable.ic_brightness_4_24dp;

            case R.string.maximize_brightness:
                return R.drawable.ic_brightness_7_24dp;

            case R.string.minimize_brightness:
                return R.drawable.ic_brightness_low_24dp;

            case R.string.notification_up:
                return R.drawable.ic_boxed_arrow_up_24dp;

            case R.string.notification_down:
                return R.drawable.ic_boxed_arrow_down_24dp;

            case R.string.toggle_flashlight:
                return R.drawable.ic_brightness_flash_light_24dp;

            case R.string.toggle_dock:
                return R.drawable.ic_arrow_collapse_down_24dp;

            case R.string.toggle_auto_rotate:
                return R.drawable.ic_auto_rotate_24dp;
        }
    }
}
