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
import android.content.Intent;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.activities.DockingActivity;

public class DockingGestureConsumer implements GestureConsumer {

    public static final String ACTION_TOGGLE_DOCK = "DockingGestureConsumer toggle dock";

    private static DockingGestureConsumer instance;

    static DockingGestureConsumer getInstance() {
        if (instance == null) instance = new DockingGestureConsumer();
        return instance;
    }

    private DockingGestureConsumer() {}

    @Override
    @SuppressLint("SwitchIntDef")
    public boolean accepts(@GestureAction int gesture) {
        return gesture == TOGGLE_DOCK;
    }

    @Override
    @SuppressLint("SwitchIntDef")
    public void onGestureActionTriggered(@GestureAction int gestureAction) {
        switch (gestureAction) {
            case TOGGLE_DOCK:
                App app = App.getInstance();
                if (app == null) return;

                Intent intent = new Intent(app, DockingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                app.startActivity(intent);
                break;
        }
    }
}

