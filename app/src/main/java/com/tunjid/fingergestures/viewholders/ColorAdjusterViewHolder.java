package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.tunjid.fingergestures.Application;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import java.util.function.Consumer;

import static com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.FLOWER;

public class ColorAdjusterViewHolder extends HomeViewHolder {

    private static final int COLOR_WHEEL_DENSITY = 12;

    private View backgroundIndicator;
    private View sliderIndicator;
    private final BrightnessGestureConsumer brightnessGestureConsumer;

    public ColorAdjusterViewHolder(View itemView) {
        super(itemView);

        brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        backgroundIndicator = itemView.findViewById(R.id.slider_background_color_indicator);
        sliderIndicator = itemView.findViewById(R.id.slider_color_indicator);

        TextView backgroundText = itemView.findViewById(R.id.slider_background_color);
        TextView sliderText = itemView.findViewById(R.id.slider_color);

        backgroundText.setText(R.string.change_slider_background_color);
        sliderText.setText(R.string.change_slider_color);

        OnClickListener backgroundPicker = view -> pickColor(brightnessGestureConsumer.getBackgroundColor(), this::setBackgroundColor);
        OnClickListener sliderPicker = view -> pickColor(brightnessGestureConsumer.getSliderColor(), this::setSliderColor);

        setBackgroundColor(brightnessGestureConsumer.getBackgroundColor());
        setSliderColor(brightnessGestureConsumer.getSliderColor());

        backgroundIndicator.setOnClickListener(backgroundPicker);
        sliderIndicator.setOnClickListener(sliderPicker);

        backgroundText.setOnClickListener(backgroundPicker);
        sliderText.setOnClickListener(sliderPicker);
    }

    private void setBackgroundColor(int color) {
        brightnessGestureConsumer.setBackgroundColor(color);
        backgroundIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    private void setSliderColor(int color) {
        brightnessGestureConsumer.setSliderColor(color);
        sliderIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    public static Drawable tint(@DrawableRes int drawableRes, int color) {
        Context context = Application.getContext();
        Drawable normalDrawable = ContextCompat.getDrawable(context, drawableRes);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, color);

        return wrapDrawable;
    }

    private void pickColor(int initialColor, Consumer<Integer> consumer) {
        ColorPickerDialogBuilder.with(itemView.getContext())
                .setPositiveButton(R.string.ok, (DialogInterface dialog, int selectedColor, Integer[] allColors) -> consumer.accept(selectedColor))
                .setNegativeButton(R.string.cancel, (DialogInterface dialog, int which) -> dialog.dismiss())
                .density(COLOR_WHEEL_DENSITY)
                .setTitle(R.string.choose_color)
                .initialColor(initialColor)
                .showColorPreview(true)
                .showAlphaSlider(false)
                .showColorEdit(true)
                .wheelType(FLOWER)
                .build()
                .show();
    }
}
