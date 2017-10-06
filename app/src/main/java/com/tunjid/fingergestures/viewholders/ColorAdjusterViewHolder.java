package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.tunjid.fingergestures.FingerGestureService;
import com.tunjid.fingergestures.R;

import java.util.function.Consumer;

import static com.tunjid.fingergestures.FingerGestureService.getBackgroundColor;
import static com.tunjid.fingergestures.FingerGestureService.getSliderColor;

public class ColorAdjusterViewHolder extends HomeViewHolder {

    private View backgroundIndicator;
    private View sliderIndicator;

    private Context context;

    public ColorAdjusterViewHolder(View itemView) {
        super(itemView);

        context = itemView.getContext();

        backgroundIndicator = itemView.findViewById(R.id.slider_background_color_indicator);
        sliderIndicator = itemView.findViewById(R.id.slider_color_indicator);

        setBackgroundColor(getBackgroundColor());
        setSliderColor(getSliderColor());

        OnClickListener backgroundPicker = view -> pickColor(getBackgroundColor(), this::setBackgroundColor);
        OnClickListener sliderPicker = view -> pickColor(getBackgroundColor(), this::setSliderColor);

        backgroundIndicator.setOnClickListener(backgroundPicker);
        sliderIndicator.setOnClickListener(sliderPicker);

        itemView.findViewById(R.id.slider_background_color).setOnClickListener(backgroundPicker);
        itemView.findViewById(R.id.slider_color).setOnClickListener(sliderPicker);
    }

    private void setBackgroundColor(int color) {
        FingerGestureService.setBackgroundColor(color);
        backgroundIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    private void setSliderColor(int color) {
        FingerGestureService.setSliderColor(color);
        sliderIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    private Drawable tint(@DrawableRes int drawableRes, int color) {
        Drawable normalDrawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, color);

        return wrapDrawable;
    }

    private void pickColor(int initialColor, Consumer<Integer> consumer) {
        ColorPickerDialogBuilder.with(new ContextThemeWrapper(context, R.style.ThemeOverlay_AppCompat_Dark))
                .setPositiveButton(R.string.ok, (DialogInterface dialog, int selectedColor, Integer[] allColors) -> consumer.accept(selectedColor))
                .setNegativeButton(R.string.cancel, (DialogInterface dialog, int which) -> dialog.dismiss())
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .setTitle(R.string.choose_color)
                .initialColor(initialColor)
                .showAlphaSlider(false)
                .density(12)
                .build()
                .show();
    }
}
