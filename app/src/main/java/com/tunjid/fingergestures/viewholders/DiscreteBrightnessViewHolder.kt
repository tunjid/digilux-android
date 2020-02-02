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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.models.Brightness
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.SLIDER_DELTA

class DiscreteBrightnessViewHolder(
        itemView: View,
        items: LiveData<List<Brightness>>,
        listener: AppAdapterListener
) : DiffViewHolder<Brightness>(itemView, items, listener) {

    override val sizeCacheKey: String
        get() = javaClass.simpleName

    init {

        val title = itemView.findViewById<TextView>(R.id.title)
        title.setText(R.string.discrete_brightness_title)
        title.setOnClickListener {
            MaterialAlertDialogBuilder(itemView.context)
                    .setMessage(R.string.discrete_brightness_description)
                    .show()
        }

        itemView.findViewById<View>(R.id.add).setOnClickListener {
            val builder = MaterialAlertDialogBuilder(itemView.context)

            if (App.canWriteToSettings()) requestDiscreteValue(builder)
            else builder.setMessage(R.string.permission_required).show()
        }
    }

    override fun bind() {
        super.bind()
        if (!App.canWriteToSettings()) listener.requestPermission(MainActivity.SETTINGS_CODE)
    }

    override fun setupRecyclerView(recyclerView: RecyclerView) = recyclerView.run {
        layoutManager = FlexboxLayoutManager(recyclerView.context).apply {
            justifyContent = JustifyContent.FLEX_START
            flexDirection = FlexDirection.ROW
            alignItems = AlignItems.CENTER
        }
        listAdapterOf(
                initialItems = items.value ?: listOf(),
                viewHolderCreator = { viewGroup, _ ->
                    DiscreteItemViewHolder(viewGroup.inflate(R.layout.viewholder_chip)) {
                        BrightnessGestureConsumer.instance.removeDiscreteBrightnessValue(it.value)
                        listener.notifyItemChanged(SLIDER_DELTA)
                        bind()
                    }
                },
                viewHolderBinder = { holder, item, _ -> holder.bind(item) }
        ).apply { adapter = this }
    }

    private fun onDiscreteValueEntered(dialogInterface: DialogInterface, editText: EditText) {
        val discreteValue = editText.text.toString()
        var value = -1

        try {
            value = Integer.valueOf(discreteValue)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isValidValue(value)) {
            BrightnessGestureConsumer.instance.addDiscreteBrightnessValue(discreteValue)
            listener.notifyItemChanged(SLIDER_DELTA)
            bind()
        }

        dialogInterface.dismiss()
    }

    private fun requestDiscreteValue(builder: MaterialAlertDialogBuilder) {
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
                .setPositiveButton(R.string.ok) { dialog, _ -> onDiscreteValueEntered(dialog, editText) }
                .create()

        editText.imeOptions = EditorInfo.IME_ACTION_SEND
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.filters = arrayOf<InputFilter>(LengthFilter(2))
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) onDiscreteValueEntered(alertDialog, editText)
            true
        }

        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        alertDialog.show()
    }

    private fun isValidValue(value: Int): Boolean {
        val invalid = value < 1 || value > 99
        if (invalid) listener.showSnackbar(R.string.discrete_brightness_error)
        return !invalid
    }
}
