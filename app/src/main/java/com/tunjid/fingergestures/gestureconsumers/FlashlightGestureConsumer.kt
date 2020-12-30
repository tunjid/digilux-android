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
import android.content.Context
import android.hardware.camera2.CameraManager
import com.tunjid.fingergestures.di.AppContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashlightGestureConsumer @Inject constructor(
    @AppContext private val context: Context
) : GestureConsumer {

    private var isCallbackRegistered: Boolean = false
    private var isTorchOn: Boolean = false

    init {
        isCallbackRegistered = registerTorchCallback(context)
    }

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean =
        gesture == GestureAction.FlashlightToggle

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(gestureAction: GestureAction) {
        when (gestureAction) {
            GestureAction.FlashlightToggle -> {
                if (!isCallbackRegistered) isCallbackRegistered = registerTorchCallback(context)
                if (!isCallbackRegistered) return

                val cameraManager = context.getSystemService(CameraManager::class.java) ?: return

                try {
                    cameraManager.setTorchMode(cameraManager.cameraIdList[0], !isTorchOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> Unit
        }
    }

    private fun registerTorchCallback(app: Context): Boolean {
        val cameraManager = app.getSystemService(CameraManager::class.java) ?: return false

        cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                isTorchOn = enabled
            }
        }, null)

        return true
    }

    //     try {
    //        for (String id : cameraManager.getCameraIdList()) {
    //            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
    //            Boolean flag = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
    //            // Turn on the flash if camera has one
    //            if (flag != null && flag) {
    //                cameraManager.setTorchMode(id, gestureAction == FLASHLIGHT_ON);
    //            }
    //        }
    //
    //    }
}

