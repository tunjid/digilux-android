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

package com.tunjid.fingergestures;

import android.content.Context;
import androidx.core.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;

@SuppressWarnings("unused") // Created via XML
public class TrialViewActionProvider extends ActionProvider {

    public TrialViewActionProvider(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        // Won't be called because of our min SDK version
        return null;
    }

    @Override
    public View onCreateActionView(MenuItem menuItem) {
        return new TrialView(getContext(), menuItem);
    }

    @Override
    public boolean onPerformDefaultAction() {
        return super.onPerformDefaultAction();
    }

}
