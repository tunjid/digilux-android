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
import com.tunjid.fingergestures.gestureconsumers.GestureAction
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

data class Unique<T>(
    val item: T,
    val time: Long = System.currentTimeMillis()
)

data class Package(val app: ApplicationInfo) : Differentiable {
    override val diffId: String get() = app.packageName

    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Package)?.let { it.diffId == diffId } ?: super.areContentsTheSame(other)
}

data class Action(val value: GestureAction, val iconColor: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Action)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

data class Brightness(val value: Int) : Differentiable {
    override val diffId: String get() = value.toString()
    override fun areContentsTheSame(other: Differentiable): Boolean =
        (other as? Brightness)?.let { it.value == value } ?: super.areContentsTheSame(other)
}

@Suppress("unused")
val Any?.ignore
    get() = Unit

fun Flowable<List<GestureAction>>.toPopUpActions(colorSource: Flowable<Int>) = Flowables.combineLatest(
    this,
    colorSource
) { gestures, color -> gestures.map { Action(it, color) } }