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

package com.tunjid.fingergestures.ui.popup


import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.core.delegates.fragmentArgs
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.baseclasses.divider
import com.tunjid.fingergestures.baseclasses.recursiveBottomSheetNavigator
import com.tunjid.fingergestures.di.activityViewModelFactory
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.gestureconsumers.GestureDirection
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Input
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.viewholders.ActionViewHolder
import com.tunjid.fingergestures.ui.main.MainViewModel

class ActionFragment : Fragment(R.layout.fragment_actions) {

    private var direction by fragmentArgs<GestureDirection?>()
    private val viewModel by viewModelFactory<ActionViewModel>()
    private val appViewModel by activityViewModelFactory<MainViewModel>()
    private val bottomSheetNavigator by recursiveBottomSheetNavigator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.title_bar).setTitle(R.string.pick_action)
        view.findViewById<RecyclerView>(R.id.options_list).apply {
            val listAdapter = listAdapterOf(
                initialItems = viewModel.state.value?.availableActions ?: listOf(),
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

            viewModel.state.apply {
                mapDistinct(ActionState::availableActions)
                    .observe(viewLifecycleOwner, listAdapter::submitList)

                mapDistinct(ActionState::needsPremium)
                    .filter(Unique<Boolean>::item)
                    .map(Unique<Boolean>::item)
                    .filterUnhandledEvents()
                    .observe(viewLifecycleOwner) { appViewModel.accept(Input.UiInteraction.GoPremium(R.string.popup_description)) }
            }
        }
    }

    private fun onActionClicked(action: Action) {
        viewModel.accept(when (val direction = direction) {
            null -> ActionInput.Add(action) // Pop up instance
            else -> ActionInput.MapGesture(direction, action)
        })

        bottomSheetNavigator.pop()
    }

    companion object {
        fun directionInstance(direction: GestureDirection): ActionFragment =
            ActionFragment().apply { this.direction = direction }

        fun popUpInstance(): ActionFragment =
            ActionFragment().apply { arguments = Bundle() }
    }
}
