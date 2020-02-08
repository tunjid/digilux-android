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

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Transformations
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

fun <T> Flowable<T>.toLiveData(errorHandler: ((Throwable) -> Unit)? = null): LiveData<T> =
        MainThreadLiveData(this, errorHandler)

fun <T, R> LiveData<T>.map(mapper: (T) -> R): LiveData<R> =
        Transformations.map(this, mapper)

fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> =
        Transformations.distinctUntilChanged(this)

/**
 * [LiveDataReactiveStreams.fromPublisher] uses [LiveData.postValue] internally which swallows
 * emissions if the occur before it can publish them using it's main thread executor.
 *
 * This class takes the reactive type, observes on the main thread, and uses [LiveData.setValue]
 * which does not swallow emissions.
 */
private class MainThreadLiveData<T>(
        val source: Flowable<T>,
        val errorHandler: ((Throwable) -> Unit)? = null
) : LiveData<T>() {

    val disposeBag = CompositeDisposable()

    override fun onActive() {
        disposeBag.clear()
        source
                .observeOn(AndroidSchedulers.mainThread())
                .run {
                    if (errorHandler == null) subscribe(this@MainThreadLiveData::setValue)
                    else subscribe(this@MainThreadLiveData::setValue, errorHandler)
                }
                .addTo(disposeBag)
    }

    override fun onInactive() = disposeBag.clear()
}
