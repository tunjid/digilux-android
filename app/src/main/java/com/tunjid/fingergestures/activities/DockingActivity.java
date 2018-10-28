package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;

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
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_TOGGLE_DOCK));
    }
}
