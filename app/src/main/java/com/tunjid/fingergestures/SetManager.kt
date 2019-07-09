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


import java.util.*
import kotlin.collections.HashSet

class SetManager<T : Any>(private val sorter: Comparator<T>,
                          private val addFilter: (String) -> Boolean,
                          private val stringMapper: (String) -> T?,
                          private val objectMapper: (T) -> String) {

    fun addToSet(value: String, preferencesName: String): Boolean {
        if (!addFilter.invoke(preferencesName)) return false

        val set = getSet(preferencesName)
        set.add(value)
        saveSet(set, preferencesName)

        return true
    }

    fun removeFromSet(packageName: String, preferencesName: String) {
        val set = getSet(preferencesName)
        set.remove(packageName)
        saveSet(set, preferencesName)
    }

    fun getList(preferenceName: String): List<String> = stream(preferenceName)

    fun getItems(preferenceName: String): List<T> = stream(preferenceName).mapNotNull(stringMapper)

    private fun stream(preferenceName: String): List<String> = getSet(preferenceName)
            .mapNotNull(stringMapper)
            .sortedWith(sorter)
            .map(objectMapper)

    fun getSet(preferencesName: String): MutableSet<String> = HashSet<String>().apply {
        val saved = App.transformApp { app -> app.preferences.getStringSet(preferencesName, emptySet())?.filterNotNull() }
        if (saved != null) addAll(saved)
    }

    private fun saveSet(set: Set<String>, preferencesName: String) =
            App.withApp { app -> app.preferences.edit().putStringSet(preferencesName, set).apply() }
}
