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

package com.tunjid.fingergestures.ui.packages

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.filterIsInstance
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.listMap
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.toLiveData
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

data class State(
    val title: String = "",
    val needsPremium: Unique<Boolean> = Unique(false),
    val installedApps: List<Package> = listOf(),
)

sealed class Input {
    data class Fetch(val preference: RotationGestureConsumer.Preference) : Input()
    data class Add(
        val preference: RotationGestureConsumer.Preference,
        val app: ApplicationInfo
    ) : Input()
}

class PackageViewModel @Inject constructor(
    @AppContext private val app: Context,
    private val rotationGestureConsumer: RotationGestureConsumer
) : ViewModel() {

    private val processor: PublishProcessor<Input> = PublishProcessor.create()

    val state = Flowable.combineLatest(
        processor.filterIsInstance<Input.Fetch>()
            .map(Input.Fetch::preference)
            .map(rotationGestureConsumer::getAddText),
        processor.filterIsInstance<Input.Add>().map { add ->
            rotationGestureConsumer.setManager
                .editorFor(add.preference)
                .plus(add.app)
                .not()
        }
            .startWith(false)
            .map(::Unique),
        processor.filterIsInstance<Input.Fetch>().concatMap {
            Flowable.fromCallable(::installedApps).subscribeOn(Schedulers.io())
        }
            .listMap(::Package)
            .startWith(listOf<Package>()),
        ::State
    )
        .toLiveData()

    fun accept(input: Input) = processor.onNext(input)

    private fun installedApps() = app.packageManager
        .getInstalledApplications(0)
        .filter(ApplicationInfo::isUserInstalledApp)
        .sortedWith(rotationGestureConsumer.applicationInfoComparator)
}

private val ApplicationInfo.isUserInstalledApp: Boolean
    get() = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || flags and ApplicationInfo.FLAG_SYSTEM == 0