package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.BRIGHTNESS_FRACTION;
import static com.tunjid.fingergestures.gestureconsumers.GestureConsumer.normalizePercentageToByte;

public class BrightnessActivity extends TimedActivity
        implements SeekBar.OnSeekBarChangeListener {

    private int brightnessByte;

    private SeekBar seekBar;
    private TextView seekBarText;
    private ViewGroup seekBarBackground;

    private BrightnessGestureConsumer brightnessGestureConsumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);

        brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();

        int sliderColor = backgroundManager.getSliderColor();
        int sliderBackgroundColor = backgroundManager.getBackgroundColor();

        ConstraintLayout layout = findViewById(R.id.constraint_layout);
        seekBarBackground = findViewById(R.id.seekbar_background);
        seekBar = findViewById(R.id.seekbar);
        seekBarText = findViewById(R.id.seekbar_text);
        ImageView settingsIcon = findViewById(R.id.go_to_settings);

        seekBarText.setTextColor(sliderColor);
        seekBar.getThumb().setColorFilter(sliderColor, SRC_IN);
        seekBar.getProgressDrawable().setColorFilter(sliderColor, SRC_IN);
        settingsIcon.setImageDrawable(backgroundManager.tint(R.drawable.ic_settings_white_24dp, sliderColor));
        seekBarBackground.setBackground(backgroundManager.tint(R.drawable.color_indicator, sliderBackgroundColor));

        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.setVerticalBias(seekBarBackground.getId(), brightnessGestureConsumer.getPositionPercentage() / 100F);
        set.applyTo(layout);

        layout.setOnClickListener(v -> finish());
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    protected void onResume() {
        super.onResume();
        if (brightnessGestureConsumer.shouldAnimateSlider())
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    @Override
    public void finish() {
        super.finish();

        boolean animateSlide = brightnessGestureConsumer.shouldAnimateSlider();
        if (animateSlide) overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
        else overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int percentage, boolean b) {
        if (percentage == 100) percentage--;
        float brightness = percentage / 100F;

        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = brightness;
        window.setAttributes(params);

        brightnessByte = normalizePercentageToByte(percentage);
        brightnessGestureConsumer.saveBrightness(brightnessByte);

        if (seekBarText.getVisibility() == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(seekBarBackground, new AutoTransition());
            seekBarText.setVisibility(View.GONE);
        }

        updateEndTime();
    }

    private void handleIntent(Intent intent) {
        TransitionManager.beginDelayedTransition(seekBarBackground, new AutoTransition());

        float brightness = intent.getFloatExtra(BRIGHTNESS_FRACTION, 0);
        int percentage = (int) (brightness * 100);

        brightnessByte = normalizePercentageToByte(percentage);
        seekBar.setProgress(percentage, true);
        seekBar.setOnSeekBarChangeListener(this);

        boolean showDimmer = brightnessGestureConsumer.shouldShowDimmer();
        seekBarText.setVisibility(showDimmer ? View.VISIBLE : View.GONE);

        if (showDimmer) {
            float dimmerPercent = brightnessGestureConsumer.getScreenDimmerDimPercent() * 100F;
            seekBarText.setText(getString(R.string.screen_dimmer_value, dimmerPercent));
        }

        waitToFinish();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        brightnessGestureConsumer.removeDimmer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
