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
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.DO_NOTHING
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_BACK
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_HOME
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_LOCK_SCREEN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_POWER_DIALOG
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_RECENTS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_SPLIT_SCREEN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.GLOBAL_TAKE_SCREENSHOT
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.INCREASE_AUDIO
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.INCREASE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.MAXIMIZE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.MINIMIZE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_DOWN
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_TOGGLE
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.NOTIFICATION_UP
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.REDUCE_AUDIO
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.REDUCE_BRIGHTNESS
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.SHOW_POPUP
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_AUTO_ROTATE
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_DOCK
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer.Companion.TOGGLE_FLASHLIGHT
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.models.Action

class ActionViewHolder(
        private val showsText: Boolean,
        itemView: View,
        private val clickListener: (Action) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private var action: Action? = null
    private val textView: TextView = itemView.findViewById(R.id.text)
    private val imageView: ImageView = itemView.findViewById(R.id.icon)

    init {
        itemView.setOnClickListener { action?.let(clickListener) }
    }

    fun bind(action: Action) {
        this.action = action

        val backgroundManager = BackgroundManager.instance

        textView.visibility = if (showsText) View.VISIBLE else View.GONE
        textView.setText(GestureMapper.instance.resourceForAction(action.value))

        val iconRes = actionToIcon(action)
        val iconColor = backgroundManager.sliderColorPreference.value

        if (showsText) imageView.setImageResource(iconRes)
        else imageView.setImageDrawable(imageView.context.drawableAt(iconRes)?.withTint(iconColor))
    }

    private fun actionToIcon(action: Action): Int = when (action.value) {
        DO_NOTHING -> R.drawable.ic_do_nothing_24dp

        INCREASE_BRIGHTNESS -> R.drawable.ic_brightness_medium_24dp

        REDUCE_BRIGHTNESS -> R.drawable.ic_brightness_4_24dp

        MAXIMIZE_BRIGHTNESS -> R.drawable.ic_brightness_7_24dp

        MINIMIZE_BRIGHTNESS -> R.drawable.ic_brightness_low_24dp

        INCREASE_AUDIO -> R.drawable.ic_volume_up_24dp

        REDUCE_AUDIO -> R.drawable.ic_volume_down_24dp

        NOTIFICATION_UP -> R.drawable.ic_boxed_arrow_up_24dp

        NOTIFICATION_DOWN -> R.drawable.ic_boxed_arrow_down_24dp

        NOTIFICATION_TOGGLE -> R.drawable.ic_boxed_arrow_up_down_24dp

        TOGGLE_FLASHLIGHT -> R.drawable.ic_brightness_flash_light_24dp

        TOGGLE_DOCK -> R.drawable.ic_arrow_collapse_down_24dp

        TOGGLE_AUTO_ROTATE -> R.drawable.ic_auto_rotate_24dp

        GLOBAL_BACK -> R.drawable.ic_back_24dp

        GLOBAL_HOME -> R.drawable.ic_home_24dp

        GLOBAL_RECENTS -> R.drawable.ic_recents_24dp

        GLOBAL_POWER_DIALOG -> R.drawable.ic_power_dialog_24dp

        GLOBAL_SPLIT_SCREEN -> R.drawable.ic_split_screen_24dp

        GLOBAL_LOCK_SCREEN -> R.drawable.ic_lock_screen_24dp

        GLOBAL_TAKE_SCREENSHOT -> R.drawable.ic_screenshot_24dp

        SHOW_POPUP -> R.drawable.ic_more_horizontal_24dp

        else -> R.drawable.ic_do_nothing_24dp
    }
}
