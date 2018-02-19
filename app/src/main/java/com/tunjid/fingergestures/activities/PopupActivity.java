package com.tunjid.fingergestures.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.adapters.DiffAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import java.util.List;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PopupActivity extends AppCompatActivity {

    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);

        BackgroundManager backgroundManager = BackgroundManager.getInstance();
        BrightnessGestureConsumer gestureConsumer = BrightnessGestureConsumer.getInstance();
        DiffAdapter adapter = new ActionAdapter(true, false, PopUpManager.getInstance()::getList, GestureMapper.getInstance()::performAction);

        List<String> actions = PopUpManager.getInstance().getList();
        int textColor = gestureConsumer.getSliderColor();
        int sliderBackgroundColor = gestureConsumer.getBackgroundColor();

        TextView text = findViewById(R.id.text);
        RecyclerView recyclerView = findViewById(R.id.item_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
        recyclerView.setBackground(backgroundManager.tint(R.drawable.color_indicator, sliderBackgroundColor));
        recyclerView.setAdapter(adapter);

        text.setTextColor(textColor);
        text.setVisibility(actions.isEmpty() ? View.VISIBLE : View.GONE);
        text.setBackground(backgroundManager.tint(R.drawable.color_indicator, sliderBackgroundColor));

        adapter.calculateDiff();

        findViewById(R.id.constraint_layout).setOnTouchListener((view, motionEvent) -> {
            finish();
            return true;
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }
}

