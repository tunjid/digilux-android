package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunjid.androidbootstrap.core.text.SpanBuilder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.GestureDirection;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;

public class MapperViewHolder extends AppViewHolder {

    @GestureDirection
    private final String doubleDirection;
    private final TextView subtitle;

    public MapperViewHolder(View itemView, @GestureDirection String direction,
                            AppAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
        GestureMapper mapper = GestureMapper.getInstance();

        this.doubleDirection = mapper.doubleDirection(direction);

        TextView title = itemView.findViewById(R.id.title);
        subtitle = itemView.findViewById(R.id.sub_title);

        title.setText(getFormattedText(direction, mapper.getMappedAction(direction)));
        subtitle.setText(getFormattedText(doubleDirection, mapper.getMappedAction(doubleDirection)));

        title.setOnClickListener(view -> onClick(mapper, title, direction));

        setIcon(itemView.findViewById(R.id.icon), direction);
    }

    @Override
    public void bind() {
        super.bind();
        GestureMapper mapper = GestureMapper.getInstance();
        subtitle.setOnClickListener(view -> {
            boolean notPremium = PurchasesManager.getInstance().isNotPremium();
            if (notPremium) goPremium(R.string.premium_prompt_double_swipe);
            else onClick(mapper, subtitle, doubleDirection);
        });
    }

    private void onClick(GestureMapper mapper, TextView textView, @GestureDirection String direction) {
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.pick_action)
                .setItems(mapper.getActions(), (a, index) -> textView.setText(getFormattedText(direction, mapper.mapGestureToAction(direction, index))))
                .show();
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
