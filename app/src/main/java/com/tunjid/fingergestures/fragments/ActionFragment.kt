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

package com.tunjid.fingergestures.fragments


import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.uidrivers.uiState
import com.tunjid.androidx.uidrivers.updatePartial
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.baseclasses.divider
import com.tunjid.fingergestures.baseclasses.mainActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.distinctUntilChanged
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.map
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.AppState
import com.tunjid.fingergestures.viewholders.ActionViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel

class ActionFragment : Fragment(R.layout.fragment_actions) {

    private val viewModel by activityViewModels<AppViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.title_bar).setTitle(R.string.pick_action)
        view.findViewById<RecyclerView>(R.id.options_list).apply {
            val listAdapter = listAdapterOf(
                    initialItems = viewModel.liveState.value?.availableActions ?: listOf(),
                    viewHolderCreator = { viewGroup, _ ->
                        ActionViewHolder(
                                showsText = true,
                                itemView = viewGroup.inflate(R.layout.viewholder_action_vertical),
                                clickListener = ::onActionClicked
                        )
                    },
                    viewHolderBinder = { holder, item, _ -> holder.bind(item) }
            )
            layoutManager = verticalLayoutManager()
            adapter = listAdapter

            addItemDecoration(divider())

            viewModel.liveState
                    .map(AppState::availableActions)
                    .distinctUntilChanged().observe(viewLifecycleOwner, listAdapter::submitList)
        }
    }

    private fun onActionClicked(action: Action) {
        val args = arguments
                ?: return ::uiState.updatePartial { copy(snackbarText = getString(R.string.generic_error)) }

        @GestureMapper.GestureDirection
        val direction = args.getString(ARG_DIRECTION)

        mainActivity.toggleBottomSheet(false)

        val mapper = GestureMapper.instance

        if (direction == null) { // Pop up instance
            val context = requireContext()
            if (!PopUpGestureConsumer.instance
                    .setManager
                    .addToSet(PopUpGestureConsumer.Preference.SavedActions, action.value.toString())) MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.go_premium_title)
                    .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.popup_description)))
                    .setPositiveButton(R.string.continue_text) { _, _ -> mainActivity.purchase(PurchasesManager.PREMIUM_SKU) }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
        } else mapper.mapGestureToAction(direction, action.value)
    }

    companion object {

        private const val ARG_DIRECTION = "DIRECTION"

        fun directionInstance(@GestureMapper.GestureDirection direction: String): ActionFragment = ActionFragment().apply {
            arguments = Bundle().apply { putString(ARG_DIRECTION, direction) }
        }

        fun popUpInstance(): ActionFragment = ActionFragment().apply { arguments = Bundle() }
    }
}
