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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.gestureconsumers.GestureAction.*
import com.tunjid.fingergestures.models.PopUp

class ActionViewHolder(
    private val showsText: Boolean,
    itemView: View,
    private val clickListener: (PopUp) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private var popUp: PopUp? = null
    private val textView: TextView = itemView.findViewById(R.id.text)
    private val imageView: ImageView = itemView.findViewById(R.id.icon)

    init {
        itemView.setOnClickListener { popUp?.let(clickListener) }
    }

    fun bind(popUp: PopUp) {
        this.popUp = popUp

        textView.isVisible = showsText
        textView.setText(popUp.value.nameRes)

        val iconRes = actionToIcon(popUp)

        if (showsText) imageView.setImageResource(iconRes)
        else imageView.setImageDrawable(imageView.context.drawableAt(iconRes)?.withTint(popUp.iconColor))
    }

    private fun actionToIcon(popUp: PopUp): Int = when (popUp.value) {
        DoNothing -> R.drawable.ic_do_nothing_24dp

        IncreaseBrightness -> R.drawable.ic_brightness_medium_24dp

        ReduceBrightness -> R.drawable.ic_brightness_4_24dp

        MaximizeBrightness -> R.drawable.ic_brightness_7_24dp

        MinimizeBrightness -> R.drawable.ic_brightness_low_24dp

        IncreaseAudio -> R.drawable.ic_volume_up_24dp

        ReduceAudio -> R.drawable.ic_volume_down_24dp

        NotificationUp -> R.drawable.ic_boxed_arrow_up_24dp

        NotificationDown -> R.drawable.ic_boxed_arrow_down_24dp

        NotificationToggle -> R.drawable.ic_boxed_arrow_up_down_24dp

        FlashlightToggle -> R.drawable.ic_brightness_flash_light_24dp

        DockToggle -> R.drawable.ic_arrow_collapse_down_24dp

        AutoRotateToggle -> R.drawable.ic_auto_rotate_24dp

        GlobalBack -> R.drawable.ic_back_24dp

        GlobalHome -> R.drawable.ic_home_24dp

        GlobalRecents -> R.drawable.ic_recents_24dp

        GlobalPowerDialog -> R.drawable.ic_power_dialog_24dp

        GlobalSplitScreen -> R.drawable.ic_split_screen_24dp

        GlobalLockScreen -> R.drawable.ic_lock_screen_24dp

        GlobalTakeScreenshot -> R.drawable.ic_screenshot_24dp

        PopUpShow -> R.drawable.ic_more_horizontal_24dp

        else -> R.drawable.ic_do_nothing_24dp
    }
}
