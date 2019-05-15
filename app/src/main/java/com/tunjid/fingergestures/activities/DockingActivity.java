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

package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;

import androidx.appcompat.app.AppCompatActivity;

import static com.tunjid.fingergestures.App.withApp;
import static com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.ACTION_TOGGLE_DOCK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DockingActivity extends AppCompatActivity {

    public static final String CONFIGURATION_CHANGE_KEY = "isConfigurationChange";
    private boolean wasMultiWindowMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wasMultiWindowMode = savedInstanceState != null && savedInstanceState.getBoolean(CONFIGURATION_CHANGE_KEY);
        setContentView(R.layout.activity_docking);
        findViewById(R.id.logo).setVisibility(isInMultiWindowMode() ? View.VISIBLE : View.GONE);
        findViewById(R.id.constraint_layout).setBackgroundColor(BackgroundManager.getInstance().getBackgroundColor());
        handleIntent(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_down_quick, R.anim.slide_out_up);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(CONFIGURATION_CHANGE_KEY, isInMultiWindowMode());
        super.onSaveInstanceState(outState);
    }

    private void handleIntent(boolean fromNewIntent) {
        if (isInMultiWindowMode() && !fromNewIntent) return;
        if (wasMultiWindowMode) finish();
        else App.delay(getDockingAnimationDuration(), MILLISECONDS, this::toggleDock);
    }

    private int getDockingAnimationDuration() {
        return getResources().getInteger(R.integer.docking_animation_duration);
    }

    private void toggleDock() {
        withApp(app -> app.broadcast(new Intent(ACTION_TOGGLE_DOCK)));
    }
}
