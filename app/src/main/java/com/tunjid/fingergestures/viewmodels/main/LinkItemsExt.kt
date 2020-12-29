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

package com.tunjid.fingergestures.viewmodels.main

import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.viewholders.ReviewLinkItem
import com.tunjid.fingergestures.viewholders.SupportLinkItem
import com.tunjid.fingergestures.viewmodels.AppViewModel
import io.reactivex.Flowable

val Inputs.linkItems: Flowable<List<Item>>
    get() = Flowable.just(listOf(
        Item.Link(
            sortKey = AppViewModel.SUPPORT,
            tab = Tab.Gestures,
            linkItem = SupportLinkItem,
            input = this@linkItems
        ),
        Item.Link(
            sortKey = AppViewModel.REVIEW,
            tab = Tab.Gestures,
            linkItem = ReviewLinkItem,
            input = this@linkItems
        ),
    ))