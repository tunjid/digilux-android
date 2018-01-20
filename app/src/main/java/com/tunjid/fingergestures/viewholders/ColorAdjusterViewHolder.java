package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.billing.PurchasesManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import java.util.function.Consumer;

import static com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.FLOWER;

public class ColorAdjusterViewHolder extends AppViewHolder {

    private static final int COLOR_WHEEL_DENSITY = 12;

    private final View backgroundIndicator;
    private final View sliderIndicator;
    private final View[] wallpaperColorIndicators;
    private final CharSequence[] targetOptions;
    private final BrightnessGestureConsumer brightnessGestureConsumer;

    public ColorAdjusterViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        Context context = itemView.getContext();
        brightnessGestureConsumer = BrightnessGestureConsumer.getInstance();
        backgroundIndicator = itemView.findViewById(R.id.slider_background_color_indicator);
        sliderIndicator = itemView.findViewById(R.id.slider_color_indicator);
        targetOptions = new CharSequence[]{context.getString(R.string.slider_background), context.getString(R.string.slider)};
        wallpaperColorIndicators = new View[]{
                itemView.findViewById(R.id.color_1),
                itemView.findViewById(R.id.color_2),
                itemView.findViewById(R.id.color_3),
                itemView.findViewById(R.id.color_4),
                itemView.findViewById(R.id.color_5),
                itemView.findViewById(R.id.color_6),
                itemView.findViewById(R.id.color_7),
        };

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

    @Override
    public void bind() {
        super.bind();
        brightnessGestureConsumer.extractPalette().subscribe(this::onPaletterExtracted, error -> {});
    }

    private void setBackgroundColor(int color) {
        brightnessGestureConsumer.setBackgroundColor(color);
        backgroundIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    private void setSliderColor(int color) {
        brightnessGestureConsumer.setSliderColor(color);
        sliderIndicator.setBackground(tint(R.drawable.color_indicator, color));
    }

    private void onPaletterExtracted(Palette palette) {
        OnClickListener pickWallpaper = this::getColorFromWallpaper;
        int defaultColor = brightnessGestureConsumer.getSliderColor();
        final int[] colors = {
                palette.getDominantColor(defaultColor),
                palette.getVibrantColor(defaultColor),
                palette.getMutedColor(defaultColor),
                palette.getDarkVibrantColor(defaultColor),
                palette.getDarkMutedColor(defaultColor),
                palette.getLightVibrantColor(defaultColor),
                palette.getLightMutedColor(defaultColor)
        };

        for (int i = 0; i < wallpaperColorIndicators.length; i++) {
            View indicator = wallpaperColorIndicators[i];
            indicator.setTag(colors[i]);
            indicator.setBackground(tint(R.drawable.color_indicator, colors[i]));
            indicator.setOnClickListener(pickWallpaper);
        }
    }

    public static Drawable tint(@DrawableRes int drawableRes, int color) {
        Context context = App.getInstance();
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

    private void getColorFromWallpaper(View indicator) {
        if (PurchasesManager.getInstance().isNotPremium()) {
            goPremium(R.string.premium_prompt_slider);
            return;
        }
        new AlertDialog.Builder(indicator.getContext())
                .setTitle(R.string.choose_target)
                .setItems(targetOptions, (dialog, position) -> {
                    int color = (int) indicator.getTag();
                    if (position == 0) setBackgroundColor(color);
                    else setSliderColor(color);
                })
                .show();
    }
}
