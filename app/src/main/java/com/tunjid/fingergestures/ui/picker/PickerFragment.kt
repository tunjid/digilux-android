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

package com.tunjid.fingergestures.ui.picker


import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.tunjid.androidx.core.delegates.fragmentArgs
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.databinding.FragmentBottomSheetBinding
import com.tunjid.fingergestures.di.activityViewModelFactory
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.gestureconsumers.GestureDirection
import com.tunjid.fingergestures.models.PopUp
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.models.liveUiState
import com.tunjid.fingergestures.ui.bottomUiClearance
import com.tunjid.fingergestures.ui.divider
import com.tunjid.fingergestures.ui.main.MainViewModel
import com.tunjid.fingergestures.ui.recursiveBottomSheetNavigator
import com.tunjid.fingergestures.viewholders.ActionViewHolder

private typealias GoPremium = com.tunjid.fingergestures.ui.main.Input.UiInteraction.GoPremium

class PickerFragment : Fragment(R.layout.fragment_bottom_sheet) {

    private var direction by fragmentArgs<GestureDirection?>()
    private val viewModel by viewModelFactory<PickerViewModel>()
    private val appViewModel by activityViewModelFactory<MainViewModel>()
    private val bottomSheetNavigator by recursiveBottomSheetNavigator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentBottomSheetBinding.bind(view)

        binding.titleBar.setTitle(R.string.pick_action)
        binding.optionsList.apply {
            val listAdapter = listAdapterOf(
                initialItems = viewModel.state.value?.availablePopUps ?: listOf(),
                viewHolderCreator = { viewGroup, _ ->
                    ActionViewHolder(
                        showsText = true,
                        itemView = viewGroup.inflate(R.layout.viewholder_action_vertical),
                        clickListener = ::onPopUpClicked
                    )
                },
                viewHolderBinder = { holder, item, _ -> holder.bind(item) }
            )
            layoutManager = verticalLayoutManager()
            adapter = listAdapter

            addItemDecoration(view.context.divider())

            liveUiState.mapDistinct(binding.root.context::bottomUiClearance).observe(viewLifecycleOwner) {
                binding.optionsList.updatePadding(bottom = it)
            }
            viewModel.state.apply {
                mapDistinct(State::availablePopUps)
                    .observe(viewLifecycleOwner, listAdapter::submitList)

                mapDistinct(State::needsPremium)
                    .filter(Unique<Boolean>::item)
                    .map(Unique<Boolean>::item)
                    .filterUnhandledEvents()
                    .observe(viewLifecycleOwner) {
                        appViewModel.accept(GoPremium(R.string.popup_description))
                    }
            }
        }
    }

    private fun onPopUpClicked(popUp: PopUp) {
        viewModel.accept(when (val direction = direction) {
            null -> Input.Add(popUp) // Pop up instance
            else -> Input.MapGesture(direction, popUp)
        })

        bottomSheetNavigator.pop()
    }

    companion object {
        fun gestureInstance(direction: GestureDirection): PickerFragment =
            PickerFragment().apply { this.direction = direction }

        fun popUpInstance(): PickerFragment =
            PickerFragment().apply { arguments = Bundle() }
    }
}
