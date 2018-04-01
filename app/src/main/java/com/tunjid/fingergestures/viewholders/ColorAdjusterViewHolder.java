package com.tunjid.fingergestures.viewholders;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.activities.MainActivity;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

import java.util.function.Consumer;

import static com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.FLOWER;
import static com.tunjid.fingergestures.App.hasStoragePermission;

public class ColorAdjusterViewHolder extends AppViewHolder {

    private static final int COLOR_WHEEL_DENSITY = 12;
    private static final int INVALID_COLOR = -1;

    private final View backgroundIndicator;
    private final View sliderIndicator;
    private final View[] wallpaperColorIndicators;
    private final CharSequence[] targetOptions;
    private final BackgroundManager backgroundManager;

    public ColorAdjusterViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);

        Context context = itemView.getContext();
        backgroundManager = BackgroundManager.getInstance();
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

        OnClickListener backgroundPicker = view -> pickColor(backgroundManager.getBackgroundColor(), this::setBackgroundColor);
        OnClickListener sliderPicker = view -> pickColor(backgroundManager.getSliderColor(), this::setSliderColor);

        setBackgroundColor(backgroundManager.getBackgroundColor());
        setSliderColor(backgroundManager.getSliderColor());

        backgroundIndicator.setOnClickListener(backgroundPicker);
        sliderIndicator.setOnClickListener(sliderPicker);

        backgroundText.setOnClickListener(backgroundPicker);
        sliderText.setOnClickListener(sliderPicker);
    }

    @Override
    public void bind() {
        super.bind();
        if (!hasStoragePermission()) adapterListener.requestPermission(MainActivity.STORAGE_CODE);
        else backgroundManager.extractPalette().subscribe(this::onPaletteExtracted, error -> {});
    }

    private void setBackgroundColor(int color) {
        backgroundManager.setBackgroundColor(color);
        backgroundIndicator.setBackground(backgroundManager.tint(R.drawable.color_indicator, color));
    }

    private void setSliderColor(int color) {
        backgroundManager.setSliderColor(color);
        sliderIndicator.setBackground(backgroundManager.tint(R.drawable.color_indicator, color));
    }

    private void onPaletteExtracted(Palette palette) {
        OnClickListener pickWallpaper = this::getColorFromWallpaper;
        final int[] colors = {
                palette.getDominantColor(INVALID_COLOR),
                palette.getVibrantColor(INVALID_COLOR),
                palette.getMutedColor(INVALID_COLOR),
                palette.getDarkVibrantColor(INVALID_COLOR),
                palette.getDarkMutedColor(INVALID_COLOR),
                palette.getLightVibrantColor(INVALID_COLOR),
                palette.getLightMutedColor(INVALID_COLOR)
        };

        for (int i = 0; i < wallpaperColorIndicators.length; i++) {
            int color = colors[i];
            View indicator = wallpaperColorIndicators[i];
            indicator.setVisibility(color == INVALID_COLOR ? View.GONE : View.VISIBLE);
            if (color == INVALID_COLOR) continue;

            indicator.setTag(colors[i]);
            indicator.setBackground(backgroundManager.tint(R.drawable.color_indicator, colors[i]));
            indicator.setOnClickListener(pickWallpaper);
        }
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
                    Object tag = indicator.getTag();
                    if (tag == null || !(tag instanceof Integer)) return;

                    int color = (int) tag;
                    if (position == 0) setBackgroundColor(color);
                    else setSliderColor(color);
                })
                .show();
    }
}
