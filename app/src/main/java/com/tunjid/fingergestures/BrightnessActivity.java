package com.tunjid.fingergestures;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.tunjid.fingergestures.FingerGestureService.BRIGHTNESS_FRACTION;
import static com.tunjid.fingergestures.FingerGestureService.getBackgroundColor;
import static com.tunjid.fingergestures.FingerGestureService.getPositionPercentage;
import static com.tunjid.fingergestures.FingerGestureService.getSliderColor;
import static com.tunjid.fingergestures.FingerGestureService.normalizePercetageToByte;
import static com.tunjid.fingergestures.FingerGestureService.saveBrightness;
import static java.util.concurrent.TimeUnit.SECONDS;

public class BrightnessActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener {

    private static final int DISMISS_DELAY = 3;

    private int brightnessByte;

    private SeekBar seekBar;
    private LocalTime endTime = LocalTime.now();
    private final AtomicReference<Disposable> reference = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ConstraintLayout layout = findViewById(R.id.constraint_layout);
        View seekBarbackground = findViewById(R.id.wrapper);
        seekBar = findViewById(R.id.seekbar);

        seekBarbackground.setBackgroundColor(getBackgroundColor());
        seekBar.getProgressDrawable().setColorFilter(getSliderColor(), SRC_IN);
        seekBar.getThumb().setColorFilter(getSliderColor(), SRC_IN);

        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.setVerticalBias(seekBarbackground.getId(), getPositionPercentage() / 100F);
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

        saveBrightness(brightnessByte);
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
        updateEndTime();
    }

    private void handleIntent(Intent intent) {
        float brightness = intent.getFloatExtra(BRIGHTNESS_FRACTION, 0);
        int percentage = (int) (brightness * 100);

        brightnessByte = normalizePercetageToByte(percentage);
        seekBar.setProgress(percentage, true);
        seekBar.setOnSeekBarChangeListener(this);

        waitToFinish();
    }

    private void waitToFinish() {
        if (reference.get() != null) return;

        reference.set(Flowable.interval(DISMISS_DELAY, SECONDS).subscribe(i -> {
                    if (LocalTime.now().isBefore(endTime)) return;
                    reference.get().dispose();
                    finish();
                }, throwable -> {}));
    }

    private void updateEndTime() {
        endTime = LocalTime.now().plusSeconds(DISMISS_DELAY);
    }

    private WindowManager.LayoutParams getWindowLayoutParams(Window window) {
        return window.getAttributes();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
