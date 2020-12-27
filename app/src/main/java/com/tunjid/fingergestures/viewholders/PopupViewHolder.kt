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

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.gridLayoutManager
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderHorizontalListBinding
import com.tunjid.fingergestures.fragments.ActionFragment
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.viewmodels.Input

private var BindingViewHolder<ViewholderHorizontalListBinding>.item by viewHolderDelegate<Item.PopUp>()
private var BindingViewHolder<ViewholderHorizontalListBinding>.listAdapter: ListAdapter<Action, ActionViewHolder> by viewHolderDelegate()

fun ViewGroup.popUp() = viewHolderFrom(ViewholderHorizontalListBinding::inflate).apply {
    binding.add.setOnClickListener {
        when {
            !App.canWriteToSettings() -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.permission_required).show()
            !item.accessibilityButtonEnabled -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.popup_prompt).show()
            else -> item.input.accept(Input.UiInteraction.ShowSheet(ActionFragment.popUpInstance()))
        }
    }
    listAdapter = listAdapterOf(
        initialItems = listOf(),
        viewHolderCreator = { viewGroup, _ ->
            ActionViewHolder(
                showsText = true,
                itemView = viewGroup.inflate(R.layout.viewholder_action_horizontal),
                clickListener = ::onActionClicked
            )
        },
        viewHolderBinder = { holder, item, _ -> holder.bind(item) }
    )

    binding.title.setText(R.string.popup_title)
    binding.title.setOnClickListener {
        MaterialAlertDialogBuilder(itemView.context)
            .setMessage(R.string.popup_description)
            .show()
    }

    binding.itemList.apply {
        layoutManager = gridLayoutManager(3)
        adapter = listAdapter
    }
}

fun BindingViewHolder<ViewholderHorizontalListBinding>.bind(item: Item.PopUp) = binding.run {
    this@bind.item = item

    if (!App.canWriteToSettings()) item.input.accept(Input.Permission.Request.Settings)
    listAdapter.submitList(item.items)
}

private fun BindingViewHolder<ViewholderHorizontalListBinding>.onActionClicked(action: Action) {
    val builder = MaterialAlertDialogBuilder(itemView.context)

    when {
        !App.canWriteToSettings() -> builder.setMessage(R.string.permission_required)
        !item.accessibilityButtonEnabled -> builder.setMessage(R.string.popup_prompt)
        else -> builder.setTitle(R.string.popup_remove)
            .setPositiveButton(R.string.yes) { _, _ ->
                item.editor.removeFromSet(PopUpGestureConsumer.Preference.SavedActions, action.value.toString())
                if (!App.canWriteToSettings()) item.input.accept(Input.Permission.Request.Settings)
            }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
    }

    builder.show()
}
