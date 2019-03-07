package com.tunjid.fingergestures.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.tunjid.androidbootstrap.recyclerview.ListManager;
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder;
import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.PopUpGestureConsumer;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.ActionAdapter;
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer;
import com.tunjid.fingergestures.gestureconsumers.GestureMapper;
import com.tunjid.fingergestures.viewholders.ActionViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
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

        List<Integer> items = new ArrayList<>();
        AtomicInteger spanSizer = new AtomicInteger(0);
        BackgroundManager backgroundManager = BackgroundManager.getInstance();

        ListManager<ActionViewHolder, Void> listManager = new ListManagerBuilder<ActionViewHolder, Void>()
                .withRecyclerView(findViewById(R.id.item_list))
                .withGridLayoutManager(6)
                .withAdapter(new ActionAdapter(true, true, items, this::onActionClicked))
                .onLayoutManager(manager -> ((GridLayoutManager) manager).setSpanSizeLookup(new SpanSizeLookup() {
                    @Override public int getSpanSize(int position) { return spanSizer.get(); }
                }))
                .build();

        TextView text = findViewById(R.id.text);
        text.setTextColor(backgroundManager.getSliderColor());
        this.<MaterialCardView>findViewById(R.id.card).setCardBackgroundColor(backgroundManager.getBackgroundColor());

        disposables.add(App.diff(items, PopUpGestureConsumer.getInstance()::getList).subscribe(result -> {
            int size = items.size();
            listManager.onDiff(result);
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

