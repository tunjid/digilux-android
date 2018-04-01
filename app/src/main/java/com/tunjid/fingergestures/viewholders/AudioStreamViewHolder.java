package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.RadioGroup;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer;

import static com.tunjid.fingergestures.activities.MainActivity.DO_NOT_DISTURB_CODE;
import static com.tunjid.fingergestures.adapters.AppAdapter.AUDIO_DELTA;


public class AudioStreamViewHolder extends AppViewHolder {

    private RadioGroup radioGroup;

    public AudioStreamViewHolder(View itemView, AppAdapter.AppAdapterListener adapterListener) {
        super(itemView, adapterListener);
        radioGroup = itemView.findViewById(R.id.radio_group);
    }

    @Override
    public void bind() {
        super.bind();
        boolean hasDoNotDisturbAccess = App.hasDoNotDisturbAccess();
        if (!hasDoNotDisturbAccess) adapterListener.requestPermission(DO_NOT_DISTURB_CODE);

        radioGroup.check(AudioGestureConsumer.getInstance().getCheckedId());
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> onStreamPicked(checkedId));

        int count = radioGroup.getChildCount();
        for (int i = 0; i < count; i++) radioGroup.getChildAt(i).setEnabled(hasDoNotDisturbAccess);
    }

    private void onStreamPicked(int checkedId) {
        AudioGestureConsumer.getInstance().setStreamType(checkedId);
        adapterListener.notifyItemChanged(AUDIO_DELTA);
    }
}
