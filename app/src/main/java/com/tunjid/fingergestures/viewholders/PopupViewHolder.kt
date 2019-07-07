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

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.ActionAdapter
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.fragments.ActionFragment
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer

class PopupViewHolder(itemView: View, items: List<Int>, listener: AppAdapter.AppAdapterListener) : DiffViewHolder<Int>(itemView, items, listener) {

    override val sizeCacheKey: String
        get() = javaClass.simpleName

    override val listSupplier: () -> List<Int>
        get() = { PopUpGestureConsumer.instance.list }

    init {

        itemView.findViewById<View>(R.id.add).setOnClickListener {
            when {
                !App.canWriteToSettings() -> AlertDialog.Builder(itemView.context).setMessage(R.string.permission_required).show()
                !PopUpGestureConsumer.instance.hasAccessibilityButton() -> AlertDialog.Builder(itemView.context).setMessage(R.string.popup_prompt).show()
                else -> adapterListener.showBottomSheetFragment(ActionFragment.popUpInstance())
            }
        }

        val title = itemView.findViewById<TextView>(R.id.title)

        title.setText(R.string.popup_title)
        title.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                    .setMessage(R.string.popup_description)
                    .show()
        }
    }

    override fun bind() {
        super.bind()

        diff()
        if (!App.canWriteToSettings()) adapterListener.requestPermission(MainActivity.SETTINGS_CODE)
    }

    override fun createListManager(itemView: View): ListManager<*, Void> {
        return ListManagerBuilder<ActionViewHolder, Void>()
                .withAdapter(ActionAdapter(isHorizontal = true, showsText = true, list = items, listener = object: ActionAdapter.ActionClickListener {
                    override fun onActionClicked(actionRes: Int) = this@PopupViewHolder.onActionClicked(actionRes)
                }))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withGridLayoutManager(3)
                .build()
    }

    private fun onActionClicked(@GestureConsumer.GestureAction action: Int) {
        val buttonManager = PopUpGestureConsumer.instance

        val builder = AlertDialog.Builder(itemView.context)

        when {
            !App.canWriteToSettings() -> builder.setMessage(R.string.permission_required)
            !buttonManager.hasAccessibilityButton() -> builder.setMessage(R.string.popup_prompt)
            else -> builder.setTitle(R.string.popup_remove)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        buttonManager.removeFromSet(action)
                        bind()
                    }
                    .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        }

        builder.show()
    }
}
