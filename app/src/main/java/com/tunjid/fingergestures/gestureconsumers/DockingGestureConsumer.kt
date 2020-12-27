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

package com.tunjid.fingergestures.gestureconsumers

import android.annotation.SuppressLint
import android.content.Intent

import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.activities.DockingActivity

class DockingGestureConsumer private constructor() : GestureConsumer {

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean {
        return gesture == GestureAction.TOGGLE_DOCK
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        when (gestureAction) {
            GestureAction.TOGGLE_DOCK -> {
                val app = App.instance ?: return

                val intent = Intent(app, DockingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                app.startActivity(intent)
            }
        }
    }

    companion object {

        const val ACTION_TOGGLE_DOCK = "DockingGestureConsumer toggle dock"

        val instance: DockingGestureConsumer by lazy { DockingGestureConsumer() }
    }
}

