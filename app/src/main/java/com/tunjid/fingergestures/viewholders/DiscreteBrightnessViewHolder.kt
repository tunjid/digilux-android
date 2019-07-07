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
import androidx.appcompat.app.AlertDialog
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.adapters.DiscreteBrightnessAdapter
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer
import com.tunjid.fingergestures.viewmodels.AppViewModel.SLIDER_DELTA

class DiscreteBrightnessViewHolder(
        itemView: View,
        items: List<String>,
        listener: AppAdapter.AppAdapterListener
) : DiffViewHolder<String>(itemView, items, listener) {

    override val sizeCacheKey: String
        get() = javaClass.simpleName

    override val listSupplier: () -> List<String>
        get() = { BrightnessGestureConsumer.getInstance().discreteBrightnessValues }

    init {

        val title = itemView.findViewById<TextView>(R.id.title)
        title.setText(R.string.discrete_brightness_title)
        title.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                    .setMessage(R.string.discrete_brightness_description)
                    .show()
        }

        itemView.findViewById<View>(R.id.add).setOnClickListener {
            val builder = AlertDialog.Builder(itemView.context)

            if (App.canWriteToSettings()) requestDiscreteValue(builder)
            else builder.setMessage(R.string.permission_required).show()
        }
    }

    override fun bind() {
        super.bind()

        diff()
        if (!App.canWriteToSettings()) adapterListener.requestPermission(MainActivity.SETTINGS_CODE)
    }

    override fun createListManager(itemView: View): ListManager<*, Void> {
        val layoutManager = FlexboxLayoutManager(itemView.context)
        layoutManager.justifyContent = JustifyContent.FLEX_START
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.alignItems = AlignItems.CENTER

        return ListManagerBuilder<DiscreteItemViewHolder, Void>()
                .withAdapter(DiscreteBrightnessAdapter(items, object : DiscreteBrightnessAdapter.BrightnessValueClickListener {
                    override fun onDiscreteBrightnessClicked(discreteValue: String) {
                        BrightnessGestureConsumer.getInstance().removeDiscreteBrightnessValue(discreteValue)
                        adapterListener.notifyItemChanged(SLIDER_DELTA)
                        bind()
                    }
                }))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withCustomLayoutManager(layoutManager)
                .build()
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
            BrightnessGestureConsumer.getInstance().addDiscreteBrightnessValue(discreteValue)
            adapterListener.notifyItemChanged(SLIDER_DELTA)
            bind()
        }

        dialogInterface.dismiss()
    }

    private fun requestDiscreteValue(builder: AlertDialog.Builder) {
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
        if (invalid) adapterListener.showSnackbar(R.string.discrete_brightness_error)
        return !invalid
    }
}
