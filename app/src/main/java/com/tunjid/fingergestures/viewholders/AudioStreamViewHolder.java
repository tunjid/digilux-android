package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.RadioGroup;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer;

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
        radioGroup.check(AudioGestureConsumer.getInstance().getCheckedId());
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> onStreamPicked(checkedId));
    }

    private void onStreamPicked(int checkedId) {
        AudioGestureConsumer.getInstance().setStreamType(checkedId);
        adapterListener.notifyItemChanged(AUDIO_DELTA);
    }
}
