package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer;

public class DockingActivity extends AppCompatActivity {

    private boolean isConfigurationChange;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docking);
        isConfigurationChange = savedInstanceState != null;
        root = findViewById(R.id.constraint_layout);

        handleIntent(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(true);
    }

    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
    }


    private void handleIntent(boolean force) {
        if (isInMultiWindowMode() && !force) return;

        if (isConfigurationChange) {
            finish();
            return;
        }
        root.postDelayed(() -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DockingGestureConsumer.ACTION_TOGGLE_DOCK)),
                1000);
    }
}
