/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.viewholders;

import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.tunjid.fingergestures.App;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer;

import static com.tunjid.fingergestures.activities.MainActivity.DO_NOT_DISTURB_CODE;
import static com.tunjid.fingergestures.viewmodels.AppViewModel.AUDIO_DELTA;


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

        AudioGestureConsumer gestureConsumer = AudioGestureConsumer.getInstance();

        radioGroup.check(gestureConsumer.getCheckedId());
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> onStreamPicked(checkedId));

        int count = radioGroup.getChildCount();

        for (int i = 0; i < count; i++) {
            View view = radioGroup.getChildAt(i);
            if (!(view instanceof RadioButton)) continue;

            RadioButton radioButton = (RadioButton) view;
            radioButton.setEnabled(hasDoNotDisturbAccess);
            radioButton.setText(gestureConsumer.getStreamTitle(radioButton.getId()));
        }
    }

    private void onStreamPicked(int checkedId) {
        AudioGestureConsumer.getInstance().setStreamType(checkedId);
        adapterListener.notifyItemChanged(AUDIO_DELTA);
    }
}
