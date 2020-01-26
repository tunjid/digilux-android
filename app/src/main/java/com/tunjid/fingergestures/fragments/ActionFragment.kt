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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.navigation.activityNavigatorController
import com.tunjid.androidx.recyclerview.acceptDiff
import com.tunjid.androidx.recyclerview.adapterOf
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.padded
import com.tunjid.fingergestures.baseclasses.MainActivityFragment
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOUBLE_DOWN_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOUBLE_LEFT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOUBLE_RIGHT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOUBLE_UP_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOWN_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.LEFT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.RIGHT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.UP_GESTURE
import com.tunjid.fingergestures.viewholders.ActionViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_DOWN_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_LEFT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_RIGHT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.MAP_UP_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.Companion.POPUP_ACTION

class ActionFragment : MainActivityFragment(R.layout.fragment_actions) {

    private val viewModel by activityViewModels<AppViewModel>()
    private val navigator by activityNavigatorController<Navigator>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.title_bar).setTitle(R.string.pick_action)
        val recyclerView = view.findViewById<RecyclerView>(R.id.options_list).apply {
            layoutManager = verticalLayoutManager()
            adapter = adapterOf(
                    itemsSource = { viewModel.state.availableActions },
                    viewHolderCreator = { viewGroup, _ ->
                        ActionViewHolder(
                                showsText = true,
                                itemView = viewGroup.inflate(R.layout.viewholder_action_vertical),
                                clickListener = ::onActionClicked
                        )
                    },
                    viewHolderBinder = { holder, item, _ -> holder.bind(item) }
            ).padded()
            addItemDecoration(divider())
        }

        disposables.add(viewModel.updatedActions().subscribe(recyclerView::acceptDiff, Throwable::printStackTrace))
    }

    private fun onActionClicked(@GestureConsumer.GestureAction actionRes: Int) {
        val args = arguments ?: return showSnackbar(R.string.generic_error)

        @GestureMapper.GestureDirection
        val direction = args.getString(ARG_DIRECTION)

        toggleBottomSheet(false)

        val fragment = navigator.current as? AppFragment ?: return

        val mapper = GestureMapper.instance

        if (direction == null) { // Pop up instance
            val context = requireContext()
            when {
                PopUpGestureConsumer.instance.addToSet(actionRes) -> fragment.notifyItemChanged(POPUP_ACTION)
                else -> AlertDialog.Builder(context)
                        .setTitle(R.string.go_premium_title)
                        .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.popup_description)))
                        .setPositiveButton(R.string.continue_text) { _, _ -> purchase(PurchasesManager.PREMIUM_SKU) }
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .show()
            }
        } else {
            mapper.mapGestureToAction(direction, actionRes)
            fragment.notifyItemChanged(when (direction) {
                LEFT_GESTURE,
                DOUBLE_LEFT_GESTURE -> MAP_LEFT_ICON
                UP_GESTURE,
                DOUBLE_UP_GESTURE -> MAP_UP_ICON
                RIGHT_GESTURE,
                DOUBLE_RIGHT_GESTURE -> MAP_RIGHT_ICON
                DOWN_GESTURE,
                DOUBLE_DOWN_GESTURE -> MAP_DOWN_ICON
                else -> MAP_DOWN_ICON
            })
        }
    }

    companion object {

        private const val ARG_DIRECTION = "DIRECTION"

        fun directionInstance(@GestureMapper.GestureDirection direction: String): ActionFragment = ActionFragment().apply {
            arguments = Bundle().apply { putString(ARG_DIRECTION, direction) }
        }

        fun popUpInstance(): ActionFragment = ActionFragment().apply { arguments = Bundle() }
    }
}
