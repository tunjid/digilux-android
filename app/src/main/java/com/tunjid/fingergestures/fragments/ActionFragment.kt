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


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.ActionAdapter
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
import com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_DOWN_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_LEFT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_RIGHT_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.MAP_UP_ICON
import com.tunjid.fingergestures.viewmodels.AppViewModel.POPUP_ACTION

class ActionFragment : MainActivityFragment(), ActionAdapter.ActionClickListener {

    private lateinit var viewModel: AppViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        viewModel = ViewModelProviders.of(requireActivity()).get(AppViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_actions, container, false) as ViewGroup

        root.findViewById<Toolbar>(R.id.title_bar).setTitle(R.string.pick_action)
        val listManager = ListManagerBuilder<ActionViewHolder, Void>()
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withLinearLayoutManager()
                .withAdapter(ActionAdapter(isHorizontal = false, showsText = true, list = viewModel.state.availableActions, listener = this))
                .addDecoration(divider())
                .build()

        disposables.add(viewModel.updatedActions().subscribe(listManager::onDiff, Throwable::printStackTrace))

        return root
    }

    override fun onActionClicked(@GestureConsumer.GestureAction actionRes: Int) {
        val args = arguments ?: return showSnackbar(R.string.generic_error)

        @GestureMapper.GestureDirection
        val direction = args.getString(ARG_DIRECTION)

        val isPopUpInstance = direction == null

        toggleBottomSheet(false)

        val fragment = currentAppFragment ?: return

        val mapper = GestureMapper.instance

        if (isPopUpInstance) {
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
            fragment.notifyItemChanged(when {
                LEFT_GESTURE == direction || DOUBLE_LEFT_GESTURE == direction -> MAP_LEFT_ICON
                UP_GESTURE == direction || DOUBLE_UP_GESTURE == direction -> MAP_UP_ICON
                RIGHT_GESTURE == direction || DOUBLE_RIGHT_GESTURE == direction -> MAP_RIGHT_ICON
                DOWN_GESTURE == direction || DOUBLE_DOWN_GESTURE == direction -> MAP_DOWN_ICON
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
