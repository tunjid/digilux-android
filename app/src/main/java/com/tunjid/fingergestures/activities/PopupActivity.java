package com.tunjid.fingergestures.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class PopupActivity extends AppCompatActivity {

    private CompositeDisposable disposables;

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

        disposables = new CompositeDisposable();

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);

        AtomicInteger spanSizer = new AtomicInteger(0);
        BackgroundManager backgroundManager = BackgroundManager.getInstance();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 6);
        ActionAdapter adapter = new ActionAdapter(true, true, PopUpGestureConsumer.getInstance()::getList, this::onActionClicked);

        TextView text = findViewById(R.id.text);
        RecyclerView recyclerView = findViewById(R.id.item_list);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        text.setTextColor(backgroundManager.getSliderColor());
        this.<MaterialCardView>findViewById(R.id.card).setCardBackgroundColor(backgroundManager.getBackgroundColor());

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) { return spanSizer.get(); }
        });

        disposables.add(adapter.calculateDiff().subscribe(size -> {
            spanSizer.set(size == 1 ? 6 : size == 2 ? 3 : 2);
            text.setVisibility(size == 0 ? VISIBLE : GONE);
        }, Throwable::printStackTrace));

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
        disposables.clear();
    }

    private void onActionClicked(@GestureConsumer.GestureAction int action) {
        finish();
        GestureMapper.getInstance().performAction(action);
    }
}

