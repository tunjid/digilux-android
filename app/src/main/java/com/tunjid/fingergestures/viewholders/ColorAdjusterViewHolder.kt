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

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.flask.colorpicker.ColorPickerView.WHEEL_TYPE.FLOWER
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ui.main.Item
import com.tunjid.fingergestures.databinding.ViewholderSliderColorBinding
import com.tunjid.fingergestures.hasStoragePermission
import com.tunjid.fingergestures.ui.main.Input

private var BindingViewHolder<ViewholderSliderColorBinding>.item by viewHolderDelegate<Item.ColorAdjuster>()
private var BindingViewHolder<ViewholderSliderColorBinding>.targetOptions by viewHolderDelegate<List<String>>()
private var BindingViewHolder<ViewholderSliderColorBinding>.wallpaperColorIndicators by viewHolderDelegate<List<View>>()

fun ViewGroup.colorAdjuster() = viewHolderFrom(ViewholderSliderColorBinding::inflate).apply {
    val context = itemView.context
    targetOptions = listOf(context.getString(R.string.slider_background), context.getString(R.string.slider))
    wallpaperColorIndicators = listOf(
        binding.color1,
        binding.color2,
        binding.color3,
        binding.color4,
        binding.color5,
        binding.color6,
        binding.color7,
    )

    binding.sliderBackgroundColor.setText(R.string.change_slider_background_color)
    binding.sliderColor.setText(R.string.change_slider_color)

    val backgroundPicker = { _: View -> context.pickColor(item.backgroundColor, item.backgroundColorSetter) }
    val sliderPicker = { _: View -> context.pickColor(item.sliderColor, item.sliderColorSetter) }

    binding.sliderBackgroundColorIndicator.setOnClickListener(backgroundPicker)
    binding.sliderBackgroundColor.setOnClickListener(backgroundPicker)
    binding.sliderColorIndicator.setOnClickListener(sliderPicker)
    binding.sliderColor.setOnClickListener(sliderPicker)
}

fun BindingViewHolder<ViewholderSliderColorBinding>.bind(item: Item.ColorAdjuster) = binding.run {
    this@bind.item = item
    val context = root.context

    sliderBackgroundColorIndicator.background = context.tintedIndicator(item.backgroundColor)
    sliderColorIndicator.background = context.tintedIndicator(item.sliderColor)

    item.palette?.let(::onPaletteExtracted)

    if (!itemView.context.hasStoragePermission) item.input.accept(Input.Permission.Request.Storage)
}

private fun BindingViewHolder<ViewholderSliderColorBinding>.onPaletteExtracted(palette: Palette) {
    val pickWallpaper = OnClickListener(::getColorFromWallpaper)

    val colors = listOf(
        palette.getDominantColor(INVALID_COLOR),
        palette.getVibrantColor(INVALID_COLOR),
        palette.getMutedColor(INVALID_COLOR),
        palette.getDarkVibrantColor(INVALID_COLOR),
        palette.getDarkMutedColor(INVALID_COLOR),
        palette.getLightVibrantColor(INVALID_COLOR),
        palette.getLightMutedColor(INVALID_COLOR)
    )

    wallpaperColorIndicators.zip(colors).forEach { (indicator, color) ->
        indicator.isVisible = color != INVALID_COLOR
        if (color != INVALID_COLOR) {
            indicator.tag = color
            indicator.background = binding.root.context.tintedIndicator(color)
            indicator.setOnClickListener(pickWallpaper)
        }
    }
}

private fun Context.tintedIndicator(@ColorInt color: Int): Drawable =
    drawableAt(R.drawable.color_indicator)
        ?.withTint(color)
        ?: ColorDrawable(color)

private fun BindingViewHolder<ViewholderSliderColorBinding>.getColorFromWallpaper(indicator: View) {
    if (!item.canPickColorFromWallpaper) return item.input.accept(Input.UiInteraction.GoPremium(R.string.premium_prompt_slider))

    MaterialAlertDialogBuilder(indicator.context)
        .setTitle(R.string.choose_target)
        .setItems(targetOptions.toTypedArray()) { _, position ->
            val tag = indicator.tag
            if (tag == null || tag !is Int) return@setItems

            if (position == 0) item.backgroundColorSetter(tag)
            else item.sliderColorSetter(tag)
        }
        .show()
}

private fun Context.pickColor(initialColor: Int, consumer: (Int) -> Unit) {
    ColorPickerDialogBuilder.with(this)
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

private const val COLOR_WHEEL_DENSITY = 12
private const val INVALID_COLOR = -1