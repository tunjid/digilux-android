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

package com.tunjid.fingergestures.gestureconsumers;

import android.annotation.SuppressLint;

public class NothingGestureConsumer implements GestureConsumer {

    @SuppressLint("StaticFieldLeak")
    private static NothingGestureConsumer instance;

    static NothingGestureConsumer getInstance() {
        if (instance == null) instance = new NothingGestureConsumer();
        return instance;
    }

    private NothingGestureConsumer() {}

    @Override
    public boolean accepts(@GestureAction int gesture) {
        return gesture == DO_NOTHING;
    }

    @Override
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        // Do nothing
    }
}

