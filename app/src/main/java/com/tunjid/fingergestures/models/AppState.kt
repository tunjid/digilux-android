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

package com.tunjid.fingergestures.models

import android.app.Application
import android.content.pm.ApplicationInfo

import com.tunjid.fingergestures.R

import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Queue

class AppState(application: Application) {

    val links: Array<TextLink>
    val brightnessValues: MutableList<String>
    val popUpActions: MutableList<Int>
    val availableActions: MutableList<Int>
    val installedApps: MutableList<ApplicationInfo>
    val rotationApps: MutableList<ApplicationInfo>
    val excludedRotationApps: MutableList<ApplicationInfo>
    val permissionsQueue: Queue<Int>

    init {
        brightnessValues = ArrayList()

        popUpActions = ArrayList()
        availableActions = ArrayList()

        installedApps = ArrayList()
        rotationApps = ArrayList()
        excludedRotationApps = ArrayList()

        permissionsQueue = ArrayDeque()

        links = arrayOf(
                TextLink(application.getString(R.string.get_set_icon), GET_SET_ICON_LINK),
                TextLink(application.getString(R.string.rxjava), RX_JAVA_LINK),
                TextLink(application.getString(R.string.color_picker), COLOR_PICKER_LINK),
                TextLink(application.getString(R.string.image_cropper), IMAGE_CROPPER_LINK),
                TextLink(application.getString(R.string.material_design_icons), MATERIAL_DESIGN_ICONS_LINK),
                TextLink(application.getString(R.string.android_bootstrap), ANDROID_BOOTSTRAP_LINK)
        )
    }

    companion object {

        private const val RX_JAVA_LINK = "https://github.com/ReactiveX/RxJava"
        private const val COLOR_PICKER_LINK = "https://github.com/QuadFlask/colorpicker"
        private const val ANDROID_BOOTSTRAP_LINK = "https://github.com/tunjid/android-bootstrap"
        private const val GET_SET_ICON_LINK = "http://www.myiconfinder.com/getseticons"
        private const val IMAGE_CROPPER_LINK = "https://github.com/ArthurHub/Android-Image-Cropper"
        private const val MATERIAL_DESIGN_ICONS_LINK = "https://materialdesignicons.com/"
    }
}
