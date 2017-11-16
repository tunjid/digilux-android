package com.tunjid.fingergestures.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.tunjid.fingergestures.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;

import static com.tunjid.fingergestures.gestureconsumers.DockingGestureConsumer.ACTION_TOGGLE_DOCK;

public class DockingActivity extends AppCompatActivity {

    public static final int DELAY = 500;
    private boolean isConfigurationChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docking);
        isConfigurationChange = savedInstanceState != null;

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

        Flowable.timer(DELAY, TimeUnit.MILLISECONDS).subscribe(ignored -> LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_TOGGLE_DOCK)), throwable -> {});
    }
}
