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

import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Package
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

val Inputs.rotationItems: Flowable<List<Item>>
    get() = with(dependencies.gestureConsumers.rotation) {
        Flowables.combineLatest(
            setManager.itemsFlowable(RotationGestureConsumer.Preference.RotatingApps),
            setManager.itemsFlowable(RotationGestureConsumer.Preference.NonRotatingApps),
            lastSeenApps.listMap(::Package),
            autoRotatePreference.monitor,
        ) { rotating, excluded, lastSeen, canAutoRotate ->
            listOf(
                Item.Toggle(
                    tab = Tab.Shortcuts,
                    sortKey = AppViewModel.ENABLE_WATCH_WINDOWS,
                    titleRes = R.string.selective_app_rotation,
                    isChecked = canAutoRotate,
                    consumer = autoRotatePreference.setter
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    sortKey = AppViewModel.ROTATION_LOCK,
                    preference = RotationGestureConsumer.Preference.RotatingApps,
                    removeText = getRemoveText(RotationGestureConsumer.Preference.RotatingApps),
                    titleRes = R.string.auto_rotate_apps,
                    infoRes = R.string.auto_rotate_description,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = setManager.editorFor(RotationGestureConsumer.Preference.RotatingApps),
                    items = rotating.sortedWith(applicationInfoComparator).map(::Package),
                    input = this@rotationItems
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    sortKey = AppViewModel.EXCLUDED_ROTATION_LOCK,
                    preference = RotationGestureConsumer.Preference.NonRotatingApps,
                    removeText = getRemoveText(RotationGestureConsumer.Preference.NonRotatingApps),
                    titleRes = R.string.auto_rotate_apps_excluded,
                    infoRes = R.string.auto_rotate_ignored_description,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = setManager.editorFor(RotationGestureConsumer.Preference.NonRotatingApps),
                    items = excluded.sortedWith(applicationInfoComparator).map(::Package),
                    input = this@rotationItems
                ),
                Item.Rotation(
                    tab = Tab.Shortcuts,
                    sortKey = AppViewModel.ROTATION_HISTORY,
                    preference = null,
                    removeText = "",
                    titleRes = R.string.app_rotation_history_title,
                    infoRes = R.string.app_rotation_history_info,
                    unRemovablePackages = unRemovablePackages,
                    canAutoRotate = canAutoRotate,
                    editor = null,
                    items = lastSeen,
                    input = this@rotationItems
                )
            )
        }
    }