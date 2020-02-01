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
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer

data class AppState(
        val links: List<TextLink> = listOf(),
        val brightnessValues: List<String> = listOf(),
        val popUpActions: List<Action> = listOf(),
        val availableActions: List<Action> = listOf(),
        val installedApps: List<Package> = listOf(),
        val rotationApps: List<Package> = listOf(),
        val excludedRotationApps: List<Package> = listOf(),
        val permissionsQueue: List<Int> = listOf()
)

data class Package(val app: ApplicationInfo) : Differentiable {
    override val diffId: String
        get() = app.packageName

    override fun areContentsTheSame(other: Differentiable): Boolean {
        return (other as? Package)?.let { it.diffId == diffId } ?: super.areContentsTheSame(other)
    }
}

inline class Action(@GestureConsumer.GestureAction val value: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
            (other as? Action)?.let { it.value == value } ?: super.areContentsTheSame(other)

}