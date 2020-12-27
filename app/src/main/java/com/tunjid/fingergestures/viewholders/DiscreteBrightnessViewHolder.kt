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
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.recyclerview.widget.ListAdapter
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.androidx.uidrivers.uiState
import com.tunjid.androidx.uidrivers.updatePartial
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.ListPreferenceEditor
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderHorizontalListBinding
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.models.Brightness
import com.tunjid.fingergestures.viewmodels.Input

private var BindingViewHolder<ViewholderHorizontalListBinding>.item by viewHolderDelegate<Item.DiscreteBrightness>()
private var BindingViewHolder<ViewholderHorizontalListBinding>.listAdapter: ListAdapter<Brightness, DiscreteItemViewHolder> by viewHolderDelegate()

fun ViewGroup.discreteBrightness() = viewHolderFrom(ViewholderHorizontalListBinding::inflate).apply {
    binding.title.setText(R.string.discrete_brightness_title)
    binding.title.setOnClickListener {
        MaterialAlertDialogBuilder(itemView.context)
            .setMessage(R.string.discrete_brightness_description)
            .show()
    }
    binding.add.setOnClickListener {
        val builder = MaterialAlertDialogBuilder(itemView.context)

        if (App.canWriteToSettings) requestDiscreteValue(builder)
        else builder.setMessage(R.string.permission_required).show()
    }
    listAdapter = listAdapterOf(
        initialItems = listOf(),
        viewHolderCreator = { viewGroup, _ ->
            DiscreteItemViewHolder(viewGroup.inflate(R.layout.viewholder_chip)) {
                item.editor.removeFromSet(BrightnessGestureConsumer.Preference.DiscreteBrightnesses, it.value)
            }
        },
        viewHolderBinder = { holder, item, _ -> holder.bind(item) }
    )
    binding.itemList.apply {
        adapter = listAdapter
        layoutManager = FlexboxLayoutManager(context).apply {
            justifyContent = JustifyContent.FLEX_START
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
        }
    }
}

fun BindingViewHolder<ViewholderHorizontalListBinding>.bind(item: Item.DiscreteBrightness) = binding.run {
    this@bind.item = item

    listAdapter.submitList(item.brightnesses)
    if (!App.canWriteToSettings) item.input.accept(Input.Permission.Request.Settings)
}

private fun BindingViewHolder<ViewholderHorizontalListBinding>.requestDiscreteValue(builder: MaterialAlertDialogBuilder) {
    val context = itemView.context

    val container = FrameLayout(context)
    val editText = EditText(context)

    val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.rightMargin = context.resources.getDimensionPixelSize(R.dimen.single_and_half_margin)
    params.leftMargin = params.rightMargin

    container.addView(editText, params)

    val alertDialog = builder.setTitle(R.string.discrete_brightness_hint)
        .setView(container)
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .setPositiveButton(R.string.ok) { dialog, _ -> editText.onDiscreteValueEntered(dialog, item.editor) }
        .create()

    editText.imeOptions = EditorInfo.IME_ACTION_SEND
    editText.inputType = InputType.TYPE_CLASS_NUMBER
    editText.filters = arrayOf<InputFilter>(LengthFilter(2))
    editText.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_SEND) editText.onDiscreteValueEntered(alertDialog, item.editor)
        true
    }

    alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    alertDialog.show()
}

private fun EditText.onDiscreteValueEntered(dialogInterface: DialogInterface, editor: ListPreferenceEditor<BrightnessGestureConsumer.Preference>) {
    text.toString()
        .toIntOrNull()
        ?.takeIf(::isValidValue)
        ?.let { editor.addToSet(BrightnessGestureConsumer.Preference.DiscreteBrightnesses, it.toString()) }

    dialogInterface.dismiss()
}

private fun EditText.isValidValue(value: Int): Boolean {
    val invalid = value < 1 || value > 99
    if (invalid) ::uiState.updatePartial { copy(snackbarText = context.getString(R.string.discrete_brightness_error)) }
    return !invalid
}
