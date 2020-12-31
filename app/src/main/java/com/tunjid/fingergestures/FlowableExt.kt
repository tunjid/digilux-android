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

package com.tunjid.fingergestures

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> Flowable<T>.asProperty(
    default: T,
    disposableHandler: (Disposable) -> Unit
) = object : ReadOnlyProperty<Any?, T> {

    private var mostRecent: T? = null

    init {
        disposableHandler(this@asProperty.subscribe(::mostRecent::set))
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = mostRecent ?: default
}

fun <T, R> Flowable<List<T>>.listMap(mapper: (T) -> R): Flowable<List<R>> =
    map { it.map(mapper) }

fun <T : Any, R> Flowable<T>.mapNotNull(mapper: (T) -> R?): Flowable<R> =
    map { Optional.ofNullable(mapper(it)) }
        .filter(Optional<R>::isPresent)
        .map(Optional<R>::get)

inline fun <reified T> Flowable<*>.filterIsInstance(): Flowable<T> =
    filter { it is T }
        .cast(T::class.java)