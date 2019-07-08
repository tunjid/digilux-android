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
import android.widget.ImageView
import android.widget.TextView
import com.tunjid.androidbootstrap.core.text.SpanBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity.Companion.ACCESSIBILITY_CODE
import com.tunjid.fingergestures.activities.MainActivity.Companion.SETTINGS_CODE
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.fragments.ActionFragment
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.DOWN_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.LEFT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.RIGHT_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.Companion.UP_GESTURE
import com.tunjid.fingergestures.gestureconsumers.GestureMapper.GestureDirection

class MapperViewHolder(
        itemView: View,
        @param:GestureDirection @field:GestureDirection private val direction: String,
        listener: AppAdapter.AppAdapterListener
) : AppViewHolder(itemView, listener) {

    private val title: TextView
    private val subtitle: TextView

    private val mapper: GestureMapper = GestureMapper.instance

    @GestureDirection
    private val doubleDirection: String

    init {
        this.doubleDirection = mapper.doubleDirection(direction)

        title = itemView.findViewById(R.id.title)
        subtitle = itemView.findViewById(R.id.sub_title)

        title.setOnClickListener { onClick(direction) }

        setIcon(itemView.findViewById(R.id.icon), direction)
    }

    override fun bind() {
        super.bind()
        if (!App.accessibilityServiceEnabled()) adapterListener.requestPermission(ACCESSIBILITY_CODE)
        if (!App.canWriteToSettings()) adapterListener.requestPermission(SETTINGS_CODE)

        title.text = getFormattedText(direction, mapper.getMappedAction(direction))
        subtitle.text = getFormattedText(doubleDirection, mapper.getMappedAction(doubleDirection))

        subtitle.setOnClickListener {
            val notPremium = PurchasesManager.instance.isNotPremium

            if (notPremium) goPremium(R.string.premium_prompt_double_swipe)
            else onClick(doubleDirection)
        }
    }

    private fun onClick(@GestureDirection direction: String) =
            adapterListener.showBottomSheetFragment(ActionFragment.directionInstance(direction))

    private fun getFormattedText(@GestureDirection direction: String, text: String): CharSequence {
        val mapper = GestureMapper.instance
        val context = itemView.context
        return SpanBuilder.format(context.getString(R.string.mapper_format),
                SpanBuilder.of(mapper.getDirectionName(direction)).bold().build(),
                text)
    }

    private fun setIcon(icon: ImageView, @GestureDirection gesture: String) {
        when (gesture) {
            UP_GESTURE -> icon.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp)
            DOWN_GESTURE -> icon.setImageResource(R.drawable.ic_app_dock_24dp)
            LEFT_GESTURE -> icon.setImageResource(R.drawable.ic_chevron_left_white_24dp)
            RIGHT_GESTURE -> icon.setImageResource(R.drawable.ic_chevron_right_white_24dp)
        }
    }
}
