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

import android.content.SharedPreferences
import com.jakewharton.rx.replayingShare
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReactivePreference<T>(
    private val preferencesName: String,
    private val default: T,
    private val onSet: ((T) -> Unit)? = null
) {
    private val listeners: MutableSet<SharedPreferences.OnSharedPreferenceChangeListener> = mutableSetOf()

    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = with(App.transformApp(App::preferences)!!) {
            when (default) {
                is String -> getString(preferencesName, default)
                is Int -> getInt(preferencesName, default)
                is Long -> getLong(preferencesName, default)
                is Float -> getFloat(preferencesName, default)
                is Boolean -> getBoolean(preferencesName, default)
                is Set<*> -> HashSet(getStringSet(preferencesName, emptySet())?.filterNotNull()
                    ?: emptySet<String>())
                else -> throw IllegalArgumentException("Uhh what are you doing?")
            }
        } as T
        set(value) = with(App.transformApp(App::preferences)!!.edit()) {
            when (value) {
                is String -> putString(preferencesName, value)
                is Int -> putInt(preferencesName, value)
                is Long -> putLong(preferencesName, value)
                is Float -> putFloat(preferencesName, value)
                is Boolean -> putBoolean(preferencesName, value)
                is Set<*> -> putStringSet(preferencesName, value.map(Any?::toString).toSet())
                else -> throw IllegalArgumentException("Uhh what are you doing?")
            }
            apply()
            onSet?.invoke(value)
        }

    val monitor: Flowable<T> = monitorPreferences().replayingShare()

    /**
     * A reference to the setter. Same as assigning to the value, but as a method reference for
     * being stable for reference equality checks
     */
    val setter = ::value::set

    val delegate: ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T = value

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            this@ReactivePreference.value = value
        }
    }

    private fun monitorPreferences(): Flowable<T> {
        val processor = BehaviorProcessor.create<T>()
        val prefs = App.transformApp(App::preferences)!!
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == preferencesName) processor.onNext(value)
        }

        return processor.subscribeOn(Schedulers.io())
            .startWith(value)
            .doOnSubscribe {
                listener.also(prefs::registerOnSharedPreferenceChangeListener)
                    .let(listeners::add)
            }
            .doFinally {
                listener.also(prefs::unregisterOnSharedPreferenceChangeListener)
                    .let(listeners::remove)
            }
    }
}
