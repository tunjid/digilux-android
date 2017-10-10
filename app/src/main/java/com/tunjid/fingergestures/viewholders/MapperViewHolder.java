package com.tunjid.fingergestures.viewholders;

import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.DOWN_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.Gesture;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.LEFT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.RIGHT_GESTURE;
import static com.tunjid.fingergestures.gestureconsumers.GestureMapper.UP_GESTURE;

public class MapperViewHolder extends HomeViewHolder {

    private final String gesture;
    private final TextView subtitle;

    public MapperViewHolder(View itemView, @Gesture String gesture) {

        super(itemView);
        this.gesture = gesture;
        GestureMapper mapper = GestureMapper.getInstance();

        itemView.<TextView>findViewById(R.id.title).setText(mapper.getGestureName(gesture));
        subtitle = itemView.findViewById(R.id.sub_title);

        subtitle.setText(mapper.getMappedAction(gesture));
        itemView.setOnClickListener(view -> onClick(mapper));
        setIcon(itemView.findViewById(R.id.icon), gesture);
    }

    private void onClick(GestureMapper mapper) {
        new AlertDialog.Builder(itemView.getContext())
                .setTitle(R.string.pick_action)
                .setItems(mapper.getActions(), (a, index) -> subtitle.setText(mapper.mapGestureToAction(gesture, index)))
                .show();
    }

    private void setIcon(ImageView icon, @Gesture String gesture) {
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
