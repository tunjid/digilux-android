package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunjid.androidbootstrap.core.text.SpanBuilder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.fragments.ActionFragment;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import static com.tunjid.fingergestures.App.accessibilityServiceEnabled;
import static com.tunjid.fingergestures.App.canWriteToSettings;
import static com.tunjid.fingergestures.activities.MainActivity.ACCESSIBILITY_CODE;
import static com.tunjid.fingergestures.activities.MainActivity.SETTINGS_CODE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.GestureDirection;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;

public class MapperViewHolder extends AppViewHolder {

    private final TextView title;
    private final TextView subtitle;

    private final GestureMapper mapper;
    @GestureDirection private final String direction;
    @GestureDirection private final String doubleDirection;

    public MapperViewHolder(View itemView, @GestureDirection String direction,
                            AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
        mapper = GestureMapper.getInstance();

        this.direction = direction;
        this.doubleDirection = mapper.doubleDirection(direction);

        title = itemView.findViewById(R.id.title);
        subtitle = itemView.findViewById(R.id.sub_title);

        title.setOnClickListener(view -> onClick(direction));

        setIcon(itemView.findViewById(R.id.icon), direction);
    }

    @Override
    public void bind() {
        super.bind();
        if (!accessibilityServiceEnabled()) adapterListener.requestPermission(ACCESSIBILITY_CODE);
        if (!canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE);

        title.setText(getFormattedText(direction, mapper.getMappedAction(direction)));
        subtitle.setText(getFormattedText(doubleDirection, mapper.getMappedAction(doubleDirection)));

        subtitle.setOnClickListener(view -> {
            boolean notPremium = PurchasesManager.getInstance().isNotPremium();
            if (notPremium) goPremium(R.string.premium_prompt_double_swipe);
            else onClick(doubleDirection);
        });
    }

    private void onClick(@GestureDirection String direction) {
        adapterListener.showBottomSheetFragment(ActionFragment.directionInstance(direction));
    }

    private CharSequence getFormattedText(@GestureDirection String direction, String text) {
        GestureMapper mapper = GestureMapper.getInstance();
        Context context = itemView.getContext();
        return SpanBuilder.format(context.getString(R.string.mapper_format),
                new SpanBuilder(context, mapper.getDirectionName(direction)).bold().build(),
                text);
    }

    private void setIcon(ImageView icon, @GestureDirection String gesture) {
        switch (gesture) {
            case UP_GESTURE:
                icon.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
                break;
            case DOWN_GESTURE:
                icon.setImageResource(R.drawable.ic_keyboard_arrow_down_white_24dp);
                break;
            case LEFT_GESTURE:
                icon.setImageResource(R.drawable.ic_chevron_left_white_24dp);
                break;
            case RIGHT_GESTURE:
                icon.setImageResource(R.drawable.ic_chevron_right_white_24dp);
                break;
        }
    }
}
