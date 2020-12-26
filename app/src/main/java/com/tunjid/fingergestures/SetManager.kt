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

import android.util.Log
import io.reactivex.Flowable
import java.util.*
import kotlin.collections.HashSet

interface ListPreferenceEditor<K : ListPreference> {
    fun addToSet(key: K, value: String): Boolean

    fun removeFromSet(key: K, packageName: String)
}

interface ListPreference {
    val preferenceName: String
}

class SetManager<K : ListPreference, T : Any>(
    keys: Iterable<K>,
    private val sorter: Comparator<T>,
    private val addFilter: (K) -> Boolean,
    private val stringMapper: (String) -> T?,
    private val objectMapper: (T) -> String
) : ListPreferenceEditor<K> {

    private val reactivePreferenceMap = keys.map { key ->
        key to ReactivePreference(key.preferenceName, emptySet<String>())
            .monitor
            .map { it.mapNotNull(stringMapper) }
    }.toMap()

    override fun addToSet(key: K, value: String): Boolean {
        if (!addFilter.invoke(key)) return false

        val set = getSet(key)
        set.add(value)
        saveSet(set, key)

        return true
    }

    override fun removeFromSet(key: K, packageName: String) {
        val set = getSet(key)
        set.remove(packageName)
        saveSet(set, key)
    }

    fun getList(key: K): List<String> = stream(key)

    fun getItems(key: K): List<T> = stream(key).mapNotNull(stringMapper)

    private fun stream(key: K): List<String> = getSet(key)
        .mapNotNull(stringMapper)
        .sortedWith(sorter)
        .map(objectMapper)

    fun getSet(key: K): MutableSet<String> = HashSet<String>().apply {
        val saved = App.transformApp { app -> app.preferences.getStringSet(key.preferenceName, emptySet())?.filterNotNull() }
        if (saved != null) addAll(saved)
    }

    private fun saveSet(set: Set<String>, key: K) =
        App.withApp { app -> app.preferences.edit().putStringSet(key.preferenceName, set).apply() }

    fun itemsFlowable(key: K): Flowable<List<T>> = reactivePreferenceMap.getValue(key)
}
