package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.viewholders.ColorAdjusterViewHolder;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.tunjid.fingergestures.gestureconsumers.GestureUtils.normalizePercetageToByte;
import static com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer.BRIGHTNESS_FRACTION;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BrightnessActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener {

    private static final int DISMISS_DELAY = 3;

    private int brightnessByte;

    private SeekBar seekBar;
    private TextView seekBarText;
    private ViewGroup seekBarbackground;

    private LocalTime endTime = LocalTime.now();
    private final AtomicReference<Disposable> reference = new AtomicReference<>();
    private BrightnessGestureConsumer brightnessGestureConsumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);

        brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ConstraintLayout layout = findViewById(R.id.constraint_layout);
        seekBarbackground = findViewById(R.id.seekbar_background);
        seekBar = findViewById(R.id.seekbar);
        seekBarText = findViewById(R.id.seekbar_text);

        seekBarbackground.setBackground(ColorAdjusterViewHolder.tint(R.drawable.color_indicator, brightnessGestureConsumer.getBackgroundColor()));
        seekBarText.setTextColor(brightnessGestureConsumer.getSliderColor());
        seekBar.getProgressDrawable().setColorFilter(brightnessGestureConsumer.getSliderColor(), SRC_IN);
        seekBar.getThumb().setColorFilter(brightnessGestureConsumer.getSliderColor(), SRC_IN);

        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.setVerticalBias(seekBarbackground.getId(), brightnessGestureConsumer.getPositionPercentage() / 100F);
        set.applyTo(layout);

        layout.setOnClickListener(v -> finish());

        updateEndTime();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }

    @Override
    public void finish() {
        Disposable disposable = reference.get();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        brightnessGestureConsumer.saveBrightness(brightnessByte);
        super.finish();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int percentage, boolean b) {
        if (percentage == 100) percentage--;
        float brightness = percentage / 100F;

        Window window = getWindow();
        WindowManager.LayoutParams params = getWindowLayoutParams(window);
        params.screenBrightness = brightness;
        window.setAttributes(params);

        brightnessByte = normalizePercetageToByte(percentage);
        brightnessGestureConsumer.saveBrightness(brightnessByte);

        if (seekBarText.getVisibility() == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(seekBarbackground, new AutoTransition());
            seekBarText.setVisibility(View.GONE);
        }

        updateEndTime();
    }

    private void handleIntent(Intent intent) {
        TransitionManager.beginDelayedTransition(seekBarbackground, new AutoTransition());

        float brightness = intent.getFloatExtra(BRIGHTNESS_FRACTION, 0);
        int percentage = (int) (brightness * 100);

        brightnessByte = normalizePercetageToByte(percentage);
        seekBar.setProgress(percentage, true);
        seekBar.setOnSeekBarChangeListener(this);

        boolean filterOn = brightnessGestureConsumer.shouldShowFilter();
        seekBarText.setVisibility(filterOn ? View.VISIBLE : View.GONE);

        if (filterOn) {
            float filterPercent = brightnessGestureConsumer.getScreenFilterDimPercent() * 100F;
            seekBarText.setText(getString(R.string.screen_filter_value, filterPercent));
        }

        waitToFinish();
    }

    private void waitToFinish() {
        if (reference.get() != null) return;
        reference.set(countdownDisposable());
    }

    @NonNull
    private Disposable countdownDisposable() {
        return Flowable.interval(DISMISS_DELAY, SECONDS).subscribe(i -> {
            if (LocalTime.now().isBefore(endTime)) return;
            reference.get().dispose();
            finish();
        }, throwable -> {});
    }

    private void updateEndTime() {
        endTime = LocalTime.now().plusSeconds(DISMISS_DELAY);
    }

    private WindowManager.LayoutParams getWindowLayoutParams(Window window) {
        return window.getAttributes();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        brightnessGestureConsumer.removeFilter();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
