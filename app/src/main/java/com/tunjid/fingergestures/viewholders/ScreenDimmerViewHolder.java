package com.tunjid.fingergestures.viewholders;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.tunjid.fingergestures.PurchasesManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;

public class ScreenDimmerViewHolder extends HomeViewHolder {

    private final Button goToSettings;
    private final Switch overLayToggle;
    private final BrightnessGestureConsumer brightnessGestureConsumer;

    public ScreenDimmerViewHolder(View itemView) {
        super(itemView);
        brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();

        goToSettings = itemView.findViewById(R.id.go_to_settings);
        overLayToggle = itemView.findViewById(R.id.toggle);

        overLayToggle.setOnCheckedChangeListener(((buttonView, isChecked) ->
                brightnessGestureConsumer.setDimmerEnabled(isChecked)
        ));
    }

    @Override
    public void bind() {
        super.bind();
        boolean hasOverlayPermission = brightnessGestureConsumer.hasOverlayPermission();

        goToSettings.setVisibility(hasOverlayPermission ? View.GONE : View.VISIBLE);
        goToSettings.setOnClickListener(this::goToSettings);

        overLayToggle.setVisibility(hasOverlayPermission ? View.VISIBLE : View.GONE);
        overLayToggle.setChecked(brightnessGestureConsumer.isDimmerEnabled());
    }

    private void goToSettings(View view) {
        if (PurchasesManager.getInstance().isNotPremium()) {
            goPremium();
            return;
        }
        Intent setiingsIntent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION);
        view.getContext().startActivity(setiingsIntent);
    }
}
