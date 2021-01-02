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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.tunjid.androidx.core.delegates.intentExtras
import com.tunjid.androidx.recyclerview.gridLayoutManager
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.databinding.ActivityPopupDialogBinding
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.mapDistinct
import com.tunjid.fingergestures.models.PopUp
import com.tunjid.fingergestures.ui.dialogLifecycleOwner
import com.tunjid.fingergestures.ui.updateVerticalBiasFor
import com.tunjid.fingergestures.viewholders.ActionViewHolder
import java.util.concurrent.atomic.AtomicInteger

class PopupDialogActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPopupDialogBinding.inflate(layoutInflater) }
    private val viewModel by viewModelFactory<PopUpViewModel>()
    private val dialogLifecycleOwner by lazy { dialogLifecycleOwner() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        end()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val window = window
        window.setLayout(MATCH_PARENT, MATCH_PARENT)

        val spanSizer = AtomicInteger(0)

        val listAdapter = listAdapterOf(
            initialItems = listOf<PopUp>(),
            viewHolderCreator = { viewGroup, _ ->
                ActionViewHolder(
                    showsText = true,
                    itemView = viewGroup.inflate(R.layout.viewholder_action_horizontal),
                    clickListener = ::onPopUpClicked
                )
            },
            viewHolderBinder = { holder, item, _ -> holder.bind(item) }
        )

        binding.itemList.apply {
            layoutManager = gridLayoutManager(6) { spanSizer.get() }
            adapter = listAdapter
        }
        binding.constraintLayout.setOnTouchListener { _, _ ->
            end()
            true
        }

        viewModel.state.apply {
            mapDistinct(State::sliderColor)
                .observe(dialogLifecycleOwner, binding.text::setTextColor)

            mapDistinct(State::backgroundColor)
                .observe(dialogLifecycleOwner, binding.card::setCardBackgroundColor)

            mapDistinct(State::verticalBias)
                .observe(
                    dialogLifecycleOwner,
                    binding.constraintLayout.updateVerticalBiasFor(binding.card.id)
                )

            mapDistinct(State::popUpActions)
                .observe(dialogLifecycleOwner) { items ->
                    val size = items.size
                    spanSizer.set(if (size == 1) 6 else if (size == 2) 3 else 2)
                    binding.text.isVisible = size == 0
                    listAdapter.submitList(items)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        when (viewModel.state.value?.animatesPopUp) {
            true -> overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun finish() {
        super.finish()
        when (viewModel.state.value?.animatesPopUp) {
            true -> overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
            else -> overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun onPopUpClicked(popUp: PopUp) {
        viewModel.accept(Input.Perform(popUp))
        end()
    }

    private fun end() = if (intent.isInBubble) onBackPressed() else finish()

    companion object {
        fun intent(context: Context, isInBubble: Boolean = false) = Intent(
            context,
            PopupDialogActivity::class.java
        ).apply {
            action = "pop up"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this.isInBubble = isInBubble
        }
    }
}

private var Intent.isInBubble by intentExtras(false)
