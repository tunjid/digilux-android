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
import android.widget.RadioButton
import android.widget.RadioGroup
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity.Companion.DO_NOT_DISTURB_CODE
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.viewmodels.AppViewModel.AUDIO_DELTA


class AudioStreamViewHolder(
        itemView: View,
        adapterListener: AppAdapter.AppAdapterListener
) : AppViewHolder(itemView, adapterListener) {

    private val radioGroup: RadioGroup = itemView.findViewById(R.id.radio_group)

    override fun bind() {
        super.bind()
        val hasDoNotDisturbAccess = App.hasDoNotDisturbAccess()
        if (!hasDoNotDisturbAccess) adapterListener.requestPermission(DO_NOT_DISTURB_CODE)

        val gestureConsumer = AudioGestureConsumer.getInstance()

        radioGroup.check(gestureConsumer.checkedId)
        radioGroup.setOnCheckedChangeListener { _, checkedId -> onStreamPicked(checkedId) }

        val count = radioGroup.childCount

        for (i in 0 until count) {
            val view = radioGroup.getChildAt(i) as? RadioButton ?: continue

            view.isEnabled = hasDoNotDisturbAccess
            view.text = gestureConsumer.getStreamTitle(view.id)
        }
    }

    private fun onStreamPicked(checkedId: Int) {
        AudioGestureConsumer.getInstance().setStreamType(checkedId)
        adapterListener.notifyItemChanged(AUDIO_DELTA)
    }
}
