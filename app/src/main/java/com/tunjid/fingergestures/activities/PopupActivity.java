package com.tunjid.fingergestures.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.adapters.DiffAdapter;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import java.util.List;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PopupActivity extends AppCompatActivity {

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);

        BackgroundManager backgroundManager = BackgroundManager.getInstance();
        DiffAdapter adapter = new ActionAdapter(true, false, PopUpGestureConsumer.getInstance()::getList, this::onActionClicked);

        List<String> actions = PopUpGestureConsumer.getInstance().getList();
        int textColor = backgroundManager.getSliderColor();
        int sliderBackgroundColor = backgroundManager.getBackgroundColor();

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

    protected void onResume() {
        super.onResume();

        if (PopUpGestureConsumer.getInstance().shouldAnimatePopup())
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        boolean shouldAnimate = PopUpGestureConsumer.getInstance().shouldAnimatePopup();
        if (shouldAnimate) overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
        else overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void onActionClicked(@GestureConsumer.GestureAction int action) {
        finish();
        GestureMapper.getInstance().performAction(action);
    }
}

