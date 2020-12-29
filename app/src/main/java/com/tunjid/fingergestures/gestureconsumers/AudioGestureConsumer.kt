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

package com.tunjid.fingergestures.gestureconsumers

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.ADJUST_LOWER
import android.media.AudioManager.ADJUST_RAISE
import androidx.annotation.IdRes
import androidx.core.content.getSystemService
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import com.tunjid.fingergestures.di.AppBroadcaster
import com.tunjid.fingergestures.di.AppContext
import com.tunjid.fingergestures.models.Broadcast
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class AudioGestureConsumer @Inject constructor(
    @AppContext private val context: Context,
    private val broadcaster: AppBroadcaster
) : GestureConsumer {

    val incrementPreference: ReactivePreference<Int> = ReactivePreference(
        preferencesName = INCREMENT_VALUE,
        default = DEF_INCREMENT_VALUE
    )
    val sliderPreference: ReactivePreference<Boolean> = ReactivePreference(
        preferencesName = SHOWS_AUDIO_SLIDER,
        default = true
    )
    val streamTypePreference: ReactivePreference<Int> = ReactivePreference(
        preferencesName = AUDIO_STREAM_TYPE,
        default = Stream.Default.type
    )

    private val audioManager get() = context.getSystemService<AudioManager>()!!

    val canSetVolumeDelta: Boolean
        get() = App.hasDoNotDisturbAccess && streamTypePreference.value != Stream.Default.type

    private val flags: Int
        get() {
            val shouldShowSlider = sliderPreference.value
            if (shouldShowSlider && streamTypePreference.value == Stream.All.type) broadcastExpandVolumeIntent()
            return if (shouldShowSlider) AudioManager.FLAG_SHOW_UI else NO_FLAGS
        }

    @SuppressLint("SwitchIntDef")
    override fun accepts(gesture: GestureAction): Boolean {
        return gesture == GestureAction.INCREASE_AUDIO || gesture == GestureAction.REDUCE_AUDIO
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(gestureAction: GestureAction) =
        adjustAudio(gestureAction == GestureAction.INCREASE_AUDIO)

    private fun setStreamVolume(increase: Boolean, audioManager: AudioManager, streamType: Int) {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val newVolume = if (increase) increase(currentVolume, streamType, audioManager) else reduce(currentVolume, streamType, audioManager)

        val ringStream = Stream.Ring.type == streamType
        val turnDnDOn = ringStream && currentVolume == 0 && !increase
        val turnDnDOff = ringStream && newVolume != 0 && increase

        if (turnDnDOn || turnDnDOff) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
                ?: return

            val filter = if (turnDnDOn) INTERRUPTION_FILTER_ALARMS else INTERRUPTION_FILTER_ALL
            if (notificationManager.currentInterruptionFilter == filter) return

            notificationManager.setInterruptionFilter(filter)
        }

        if (!turnDnDOn) audioManager.setStreamVolume(streamType, newVolume, flags)
    }

    private fun reduce(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
        max(currentValue - normalizePercentageForStream(incrementPreference.value, stream, audioManager), MIN_VOLUME)

    private fun increase(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
        min(currentValue + normalizePercentageForStream(incrementPreference.value, stream, audioManager), audioManager.getStreamMaxVolume(stream))

    private fun adjustAudio(increase: Boolean) {
        if (!App.hasDoNotDisturbAccess) return
        when (val stream = Stream.forType(streamTypePreference.value)) {
            Stream.Default -> audioManager.adjustSuggestedStreamVolume(if (increase) ADJUST_RAISE else ADJUST_LOWER, stream.type, flags)
            Stream.Media, Stream.Alarm, Stream.Ring -> setStreamVolume(increase, audioManager, stream.type)
            Stream.All -> {
                setStreamVolume(increase, audioManager, Stream.Media.type)
                setStreamVolume(increase, audioManager, Stream.Ring.type)
                setStreamVolume(increase, audioManager, Stream.Alarm.type)
            }
            else -> audioManager.adjustSuggestedStreamVolume(if (increase) ADJUST_RAISE else ADJUST_LOWER, stream.type, flags)
        }
    }

    fun getChangeText(percentage: Int): String {
        if (!App.hasDoNotDisturbAccess) return context.getString(R.string.enable_do_not_disturb)
        val normalized = normalizePercentageForStream(percentage, streamTypePreference.value, audioManager)

        return when (val maxSteps = getMaxSteps(audioManager)) {
            MANAGED_BY_SYSTEM -> context.getString(R.string.audio_stream_text_system)
            else -> context.getString(R.string.audio_stream_text, normalized, maxSteps)
        }
    }

    fun getStreamTitle(@IdRes resId: Int): String = when (val stream = Stream.forId(resId)) {
        Stream.Default -> context.getString(R.string.audio_stream_default)
        Stream.Media -> context.getString(R.string.audio_stream_media, audioManager.getStreamMaxVolume(stream.type))
        Stream.Ring -> context.getString(R.string.audio_stream_ringtone, audioManager.getStreamMaxVolume(stream.type))
        Stream.Alarm -> context.getString(R.string.audio_stream_alarm, audioManager.getStreamMaxVolume(stream.type))
        Stream.All -> context.getString(R.string.audio_stream_all, getMaxSteps(audioManager))
        else -> context.getString(R.string.audio_stream_default)
    }

    private fun normalizePercentageForStream(percentage: Int, streamType: Int, audioManager: AudioManager): Int {
        val actualStreamType: Int = when (Stream.forType(streamType)) {
            Stream.Media, Stream.Ring, Stream.Alarm -> streamType
            else -> Stream.Media.type
        }

        val streamMax = audioManager.getStreamMaxVolume(actualStreamType)
        return max(percentage * streamMax / 100, 1)
    }

    private fun broadcastExpandVolumeIntent() {
        App.delay(EXPAND_VOLUME_DELAY, MILLISECONDS) { broadcaster(Broadcast.Service.ExpandVolumeControls) }
    }

    private fun getStreamWithLargestMax(audioManager: AudioManager): Int {
        val max = max(audioManager.getStreamMaxVolume(Stream.Alarm.type), audioManager.getStreamMaxVolume(Stream.Ring.type))
        return max(max, audioManager.getStreamMaxVolume(Stream.Media.type))
    }

    private fun getMaxSteps(audioManager: AudioManager): Int = when (val stream = Stream.forType(streamTypePreference.value)) {
        Stream.Media, Stream.Ring, Stream.Alarm -> audioManager.getStreamMaxVolume(stream.type)
        Stream.All -> getStreamWithLargestMax(audioManager)
        else -> MANAGED_BY_SYSTEM
    }

    enum class Stream(
        val id: Int,
        val type: Int
    ) {
        Default(
            id = R.id.stream_default,
            type = AudioManager.USE_DEFAULT_STREAM_TYPE
        ),
        Media(
            id = R.id.stream_media,
            type = AudioManager.STREAM_MUSIC
        ),
        Ring(
            id = R.id.stream_ring,
            type = AudioManager.STREAM_RING
        ),
        Alarm(
            id = R.id.stream_alarm,
            type = AudioManager.STREAM_ALARM
        ),
        All(
            id = R.id.stream_all,
            type = Integer.MAX_VALUE
        );

        companion object {
            fun forId(id: Int) = values().firstOrNull { it.id == id } ?: Default
            fun forType(type: Int) = values().firstOrNull { it.type == type } ?: Default
        }
    }

    companion object {

        private const val NO_FLAGS = 0
        private const val MIN_VOLUME = 0
        private const val DEF_INCREMENT_VALUE = 20
        private const val EXPAND_VOLUME_DELAY = 200L

        //        private const val STREAM_TYPE_MEDIA = AudioManager.STREAM_MUSIC
//        private const val STREAM_TYPE_RING = AudioManager.STREAM_RING
//        private const val STREAM_TYPE_ALARM = AudioManager.STREAM_ALARM
//        private const val STREAM_TYPE_DEFAULT = AudioManager.USE_DEFAULT_STREAM_TYPE
//        private const val STREAM_TYPE_ALL = Integer.MAX_VALUE
        private const val MANAGED_BY_SYSTEM = -1

        private const val INCREMENT_VALUE = "audio increment value"
        private const val AUDIO_STREAM_TYPE = "audio stream type"
        private const val SHOWS_AUDIO_SLIDER = "audio slider show"
    }
}

//    public void setVolume(@IdRes int idRes, @IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int percentage) {
//        AudioManager audioManager = App.requireApp(app -> app.getSystemService(AudioManager.class), null);
//        if (audioManager == null) return;
//
//        @AudioStream int streamType = getStreamTypeFromId(idRes);
//
//        switch (streamType) {
//            default:
//            case STREAM_TYPE_DEFAULT:
//                // No op
//                break;
//            case STREAM_TYPE_MEDIA:
//            case STREAM_TYPE_RING:
//            case STREAM_TYPE_ALARM:
//                setStreamVolume(percentage, audioManager, streamType);
//                break;
//            case STREAM_TYPE_ALL:
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_MEDIA);
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_RING);
//                setStreamVolume(percentage, audioManager, STREAM_TYPE_ALARM);
//                break;
//        }
//    }
//
//    private void setStreamVolume(@IntRange(from = ZERO_PERCENT, to = HUNDRED_PERCENT) int percentage, AudioManager audioManager, int streamType) {
//        int newVolume = normalizePercentageForStream(percentage, streamType, audioManager);
//        audioManager.setStreamVolume(streamType, newVolume, getFlags());
//    }