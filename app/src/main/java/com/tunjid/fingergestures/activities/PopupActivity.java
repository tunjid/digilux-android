package com.tunjid.fingergestures.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Window;

import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpManager;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.gestureconsumers.BrightnessGestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PopupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);

        PopUpManager buttonManager = PopUpManager.getInstance();
        int sliderBackgroundColor = BrightnessGestureConsumer.getInstance().getBackgroundColor();

        RecyclerView recyclerView = findViewById(R.id.item_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
        recyclerView.setBackground(BackgroundManager.getInstance().tint(R.drawable.color_indicator, sliderBackgroundColor));
        recyclerView.setAdapter(new ActionAdapter(true, false, buttonManager.getList(), GestureMapper.getInstance()::performAction));

        findViewById(R.id.constraint_layout).setOnClickListener(v -> finish());
    }

    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}

