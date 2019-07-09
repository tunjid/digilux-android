/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.viewholders

import android.content.DialogInterface
import android.view.View
import android.view.View.OnClickListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.palette.graphics.Palette
import com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.FLOWER
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.billing.PurchasesManager

class ColorAdjusterViewHolder(
        itemView: View,
        listener: AppAdapter.AppAdapterListener
) : AppViewHolder(itemView, listener) {

    private val backgroundIndicator: View
    private val sliderIndicator: View
    private val wallpaperColorIndicators: Array<View>
    private val targetOptions: Array<CharSequence>
    private val backgroundManager: BackgroundManager

    init {
        val context = itemView.context

        backgroundManager = BackgroundManager.instance

        backgroundIndicator = itemView.findViewById(R.id.slider_background_color_indicator)
        sliderIndicator = itemView.findViewById(R.id.slider_color_indicator)

        targetOptions = arrayOf(context.getString(R.string.slider_background), context.getString(R.string.slider))
        wallpaperColorIndicators = arrayOf(
                itemView.findViewById(R.id.color_1),
                itemView.findViewById(R.id.color_2),
                itemView.findViewById(R.id.color_3),
                itemView.findViewById(R.id.color_4),
                itemView.findViewById(R.id.color_5),
                itemView.findViewById(R.id.color_6), itemView.findViewById(R.id.color_7)
        )

        val backgroundText = itemView.findViewById<TextView>(R.id.slider_background_color)
        val sliderText = itemView.findViewById<TextView>(R.id.slider_color)

        backgroundText.setText(R.string.change_slider_background_color)
        sliderText.setText(R.string.change_slider_color)

        val backgroundPicker = { _: View -> pickColor(backgroundManager.backgroundColor, this::setBackgroundColor) }
        val sliderPicker = { _: View -> pickColor(backgroundManager.sliderColor, this::setSliderColor) }

        setBackgroundColor(backgroundManager.backgroundColor)
        setSliderColor(backgroundManager.sliderColor)

        backgroundIndicator.setOnClickListener(backgroundPicker)
        sliderIndicator.setOnClickListener(sliderPicker)

        backgroundText.setOnClickListener(backgroundPicker)
        sliderText.setOnClickListener(sliderPicker)
    }

    override fun bind() {
        super.bind()
        if (!App.hasStoragePermission) adapterListener.requestPermission(MainActivity.STORAGE_CODE)
        else backgroundManager.extractPalette().subscribe(this::onPaletteExtracted, Throwable::printStackTrace)
    }

    private fun setBackgroundColor(color: Int) {
        backgroundManager.backgroundColor = color
        backgroundIndicator.background = backgroundManager.tint(R.drawable.color_indicator, color)
    }

    private fun setSliderColor(color: Int) {
        backgroundManager.sliderColor = color
        sliderIndicator.background = backgroundManager.tint(R.drawable.color_indicator, color)
    }

    private fun onPaletteExtracted(palette: Palette) {
        val pickWallpaper = OnClickListener(this::getColorFromWallpaper)
        val colors = intArrayOf(
                palette.getDominantColor(INVALID_COLOR),
                palette.getVibrantColor(INVALID_COLOR),
                palette.getMutedColor(INVALID_COLOR),
                palette.getDarkVibrantColor(INVALID_COLOR),
                palette.getDarkMutedColor(INVALID_COLOR),
                palette.getLightVibrantColor(INVALID_COLOR),
                palette.getLightMutedColor(INVALID_COLOR)
        )

        for (i in wallpaperColorIndicators.indices) {
            val color = colors[i]
            val indicator = wallpaperColorIndicators[i]
            indicator.visibility = if (color == INVALID_COLOR) View.GONE else View.VISIBLE
            if (color == INVALID_COLOR) continue

            indicator.tag = colors[i]
            indicator.background = backgroundManager.tint(R.drawable.color_indicator, colors[i])
            indicator.setOnClickListener(pickWallpaper)
        }
    }

    private fun pickColor(initialColor: Int, consumer: (Int) -> Unit) {
        ColorPickerDialogBuilder.with(itemView.context)
                .setPositiveButton(R.string.ok) { _: DialogInterface, selectedColor: Int, _: Array<Int> -> consumer.invoke(selectedColor) }
                .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .density(COLOR_WHEEL_DENSITY)
                .setTitle(R.string.choose_color)
                .initialColor(initialColor)
                .showColorPreview(true)
                .showAlphaSlider(false)
                .showColorEdit(true)
                .wheelType(FLOWER)
                .build()
                .show()
    }

    private fun getColorFromWallpaper(indicator: View) {
        if (PurchasesManager.instance.isNotPremium) return goPremium(R.string.premium_prompt_slider)

        AlertDialog.Builder(indicator.context)
                .setTitle(R.string.choose_target)
                .setItems(targetOptions) { _, position ->
                    val tag = indicator.tag
                    if (tag == null || tag !is Int) return@setItems

                    if (position == 0) setBackgroundColor(tag)
                    else setSliderColor(tag)
                }
                .show()
    }

    companion object {

        private const val COLOR_WHEEL_DENSITY = 12
        private const val INVALID_COLOR = -1
    }
}
