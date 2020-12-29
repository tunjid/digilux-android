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
import java.util.*
import kotlin.collections.HashSet

interface SetPreferenceEditor<V : Any> {
    /**
     * Adds an item to the set.
     * Returns false if the item could not be added for whatever reason.
     */
    operator fun plus(value: V): Boolean

    /**
     * Removes an item from the set
     */
    operator fun minus(value: V)
}

interface SetPreference {
    val preferenceName: String
}

class SetManager<K : SetPreference, V : Any>(
    keys: Iterable<K>,
    reactivePreferences: ReactivePreferences,
    private val sorter: Comparator<V>,
    private val addFilter: (K) -> Boolean,
    private val stringMapper: (String) -> V?,
    private val objectMapper: (V) -> String
) {

    private val reactivePreferenceMap = keys.map { key ->
        key to ReactivePreference(reactivePreferences, key.preferenceName, emptySet<String>())
            .monitor
            .map { it.mapNotNull(stringMapper) }
    }.toMap()

    private val editorMap = keys.map { key ->
        key to object : SetPreferenceEditor<V> {
            override fun plus(value: V) = addToSet(key, value)

            override fun minus(value: V) = removeFromSet(key, value)
        }
    }.toMap()

    fun editorFor(key: K) = editorMap.getValue(key)

    private fun addToSet(key: K, value: V): Boolean {
        if (!addFilter.invoke(key)) return false

        val set = getSet(key)
        set.add(objectMapper(value))
        saveSet(set, key)

        return true
    }

    private fun removeFromSet(key: K, value: V) {
        val set = getSet(key)
        set.remove(objectMapper(value))
        saveSet(set, key)
    }

    fun getItems(key: K): List<V> = stream(key).mapNotNull(stringMapper)

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

    fun itemsFlowable(key: K): Flowable<List<V>> = reactivePreferenceMap.getValue(key)
}
