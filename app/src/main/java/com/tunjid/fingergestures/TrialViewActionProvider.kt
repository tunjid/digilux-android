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

import android.content.Context
import androidx.core.view.ActionProvider
import android.view.MenuItem
import android.view.View

// Created via XML
@Suppress("unused")
class TrialViewActionProvider(context: Context) : ActionProvider(context) {

    // Won't be called because of our min SDK version
    override fun onCreateActionView(): View? = null

    override fun onCreateActionView(menuItem: MenuItem?): View = TrialView(context, menuItem!!)

}
