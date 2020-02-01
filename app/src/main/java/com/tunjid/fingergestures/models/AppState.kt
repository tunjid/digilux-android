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

import android.content.pm.ApplicationInfo

data class AppState(
        val links: List<TextLink> = listOf(),
        val brightnessValues: List<String> = listOf(),
        val popUpActions: List<Int> = listOf(),
        val availableActions: List<Int> = listOf(),
        val installedApps: List<ApplicationInfo> = listOf(),
        val rotationApps: List<ApplicationInfo> = listOf(),
        val excludedRotationApps: List<ApplicationInfo> = listOf(),
        val permissionsQueue: List<Int> = listOf()
)
