package com.tunjid.fingergestures.viewholders;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.HomeAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;

public class ScreenDimmerViewHolder extends HomeViewHolder {

    private final Button goToSettings;
    private final Switch overLayToggle;
    private final PurchasesManager purchasesManager;
    private final BrightnessGestureConsumer brightnessGestureConsumer;

    public ScreenDimmerViewHolder(View itemView, HomeAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
        purchasesManager = PurchasesManager.getInstance();
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
        boolean isPremium = !purchasesManager.isNotPremium();
        boolean hasOverlayPermission = brightnessGestureConsumer.hasOverlayPermission();

        goToSettings.setVisibility(hasOverlayPermission ? View.GONE : View.VISIBLE);
        goToSettings.setOnClickListener(this::goToSettings);

        overLayToggle.setEnabled(isPremium);
        overLayToggle.setVisibility(hasOverlayPermission ? View.VISIBLE : View.GONE);
        overLayToggle.setText(isPremium ? R.string.screen_dimmer_toggle : R.string.go_premium_text);
        overLayToggle.setChecked(brightnessGestureConsumer.isDimmerEnabled());

        if (!isPremium) itemView.setOnClickListener(this::goToSettings);
    }

    private void goToSettings(View view) {
        if (purchasesManager.isNotPremium()) {
            goPremium(R.string.premium_prompt_dimmer);
            return;
        }
        Intent setiingsIntent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION);
        view.getContext().startActivity(setiingsIntent);
    }
}
