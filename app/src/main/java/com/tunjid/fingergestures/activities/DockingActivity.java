package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;

import static com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.ACTION_TOGGLE_DOCK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DockingActivity extends AppCompatActivity {

    public static final int DELAY = 200;
    private boolean isConfigurationChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConfigurationChange = savedInstanceState != null;
        setContentView(R.layout.activity_docking);
        findViewById(R.id.logo).setVisibility(isInMultiWindowMode() ? View.VISIBLE : View.GONE);
        findViewById(R.id.constraint_layout).setBackgroundColor(BrightnessGestureConsumer.getInstance().getBackgroundColor());
        handleIntent(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(true);
    }

    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_down_quick, R.anim.slide_out_up);
    }


    private void handleIntent(boolean force) {
        if (isInMultiWindowMode() && !force) return;
        if (isConfigurationChange) finish();
        else App.delay(DELAY, MILLISECONDS, this::toggleDock);
    }

    private void toggleDock() {
         LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_TOGGLE_DOCK));
    }
}
