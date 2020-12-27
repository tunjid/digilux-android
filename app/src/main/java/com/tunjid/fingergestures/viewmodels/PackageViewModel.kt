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

package com.tunjid.fingergestures.viewmodels

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.toLiveData
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

data class PackageState(
    val needsPremium: Unique<Boolean> = Unique(false),
    val installedApps: List<Package> = listOf(),
)

sealed class PackageInput {
    data class FetchApps(val time: Long = System.currentTimeMillis()) : PackageInput()
    data class Add(
        val preference: RotationGestureConsumer.Preference,
        val app: ApplicationInfo
    ) : PackageInput()
}

class PackageViewModel(application: Application) : AndroidViewModel(application) {

    private val processor: PublishProcessor<PackageInput> = PublishProcessor.create()

    val state = Flowable.combineLatest(
        processor.filterIsInstance<PackageInput.Add>().map { add ->
            RotationGestureConsumer.instance.setManager
                .editorFor(add.preference)
                .plus(add.app)
                .not()
        }
            .startWith(false)
            .map(::Unique),
        processor.filterIsInstance<PackageInput.FetchApps>().concatMap {
            Flowable.fromCallable {
                getApplication<Application>().packageManager.getInstalledApplications(0)
                    .filter(ApplicationInfo::isUserInstalledApp)
                    .sortedWith(RotationGestureConsumer.instance.applicationInfoComparator)
            }
        }
            .listMap(::Package)
            .startWith(listOf<Package>()),
        ::PackageState
    )
        .toLiveData()


    fun accept(input: PackageInput) = processor.onNext(input)
}

private val ApplicationInfo.isUserInstalledApp: Boolean
    get() = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 || flags and ApplicationInfo.FLAG_SYSTEM == 0