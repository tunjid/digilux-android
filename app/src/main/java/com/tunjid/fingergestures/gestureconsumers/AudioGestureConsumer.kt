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
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.ADJUST_LOWER
import android.media.AudioManager.ADJUST_RAISE
import androidx.annotation.IdRes
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.ReactivePreference
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.max
import kotlin.math.min

class AudioGestureConsumer private constructor() : GestureConsumer {

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

    val canSetVolumeDelta: Boolean
        get() = App.hasDoNotDisturbAccess() && streamTypePreference.value != Stream.Default.type

    private val flags: Int
        get() {
            val shouldShowSlider = sliderPreference.value
            if (shouldShowSlider && streamTypePreference.value == Stream.All.type) broadcastExpandVolumeIntent()
            return if (shouldShowSlider) AudioManager.FLAG_SHOW_UI else NO_FLAGS
        }

    @SuppressLint("SwitchIntDef")
    override fun accepts(@GestureConsumer.GestureAction gesture: Int): Boolean {
        return gesture == GestureConsumer.INCREASE_AUDIO || gesture == GestureConsumer.REDUCE_AUDIO
    }

    @SuppressLint("SwitchIntDef")
    override fun onGestureActionTriggered(@GestureConsumer.GestureAction gestureAction: Int) =
        adjustAudio(gestureAction == GestureConsumer.INCREASE_AUDIO)

    private fun setStreamVolume(increase: Boolean, audioManager: AudioManager, streamType: Int) {
        val currentVolume = audioManager.getStreamVolume(streamType)
        val newVolume = if (increase) increase(currentVolume, streamType, audioManager) else reduce(currentVolume, streamType, audioManager)

        val ringStream = Stream.Ring.type == streamType
        val turnDnDOn = ringStream && currentVolume == 0 && !increase
        val turnDnDOff = ringStream && newVolume != 0 && increase

        if (turnDnDOn || turnDnDOff) App.withApp { app ->
            val notificationManager = app.getSystemService(NotificationManager::class.java)
                ?: return@withApp

            val filter = if (turnDnDOn) INTERRUPTION_FILTER_ALARMS else INTERRUPTION_FILTER_ALL
            if (notificationManager.currentInterruptionFilter == filter) return@withApp

            notificationManager.setInterruptionFilter(filter)
        }

        if (!turnDnDOn) audioManager.setStreamVolume(streamType, newVolume, flags)
    }

    private fun reduce(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
        max(currentValue - normalizePercentageForStream(incrementPreference.value, stream, audioManager), MIN_VOLUME)

    private fun increase(currentValue: Int, stream: Int, audioManager: AudioManager): Int =
        min(currentValue + normalizePercentageForStream(incrementPreference.value, stream, audioManager), audioManager.getStreamMaxVolume(stream))

    private fun adjustAudio(increase: Boolean) {
        requireAppAndAudioManager({ _, audioManager ->
            if (!App.hasDoNotDisturbAccess()) return@requireAppAndAudioManager Void.TYPE
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
            Void.TYPE
        }, Void.TYPE)
    }

    fun getChangeText(percentage: Int): String {
        return requireAppAndAudioManager({ app, audioManager ->
            if (!App.hasDoNotDisturbAccess()) return@requireAppAndAudioManager app.getString(R.string.enable_do_not_disturb)

            val normalized = normalizePercentageForStream(percentage, streamTypePreference.value, audioManager)
            val maxSteps = getMaxSteps(audioManager)

            if (maxSteps == MANAGED_BY_SYSTEM)
                app.getString(R.string.audio_stream_text_system)
            else
                app.getString(R.string.audio_stream_text, normalized, maxSteps)
        }, EMPTY_STRING)
    }

    fun getStreamTitle(@IdRes resId: Int): String = when (val stream = Stream.forId(resId)) {
        Stream.Default -> App.transformApp({ app -> app.getString(R.string.audio_stream_default) }, EMPTY_STRING)
        Stream.Media -> requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_media, audioManager.getStreamMaxVolume(stream.type)) }, EMPTY_STRING)
        Stream.Ring -> requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_ringtone, audioManager.getStreamMaxVolume(stream.type)) }, EMPTY_STRING)
        Stream.Alarm -> requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_alarm, audioManager.getStreamMaxVolume(stream.type)) }, EMPTY_STRING)
        Stream.All -> requireAppAndAudioManager({ app, audioManager -> app.getString(R.string.audio_stream_all, getMaxSteps(audioManager)) }, EMPTY_STRING)
        else -> App.transformApp({ app -> app.getString(R.string.audio_stream_default) }, EMPTY_STRING)
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
        val expandVolumeIntent = Intent(ACTION_EXPAND_VOLUME_CONTROLS)
        App.delay(EXPAND_VOLUME_DELAY, MILLISECONDS) { App.withApp { app -> app.broadcast(expandVolumeIntent) } }
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

    private fun <T> requireAppAndAudioManager(biFunction: (App, AudioManager) -> T, defaultValue: T): T =
        App.transformApp({ app ->
            val audioManager = app.getSystemService(AudioManager::class.java)
            if (audioManager != null) biFunction.invoke(app, audioManager) else defaultValue
        }, defaultValue)

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

        const val ACTION_EXPAND_VOLUME_CONTROLS = "AudioGestureConsumer expand volume"
        private const val INCREMENT_VALUE = "audio increment value"
        private const val AUDIO_STREAM_TYPE = "audio stream type"
        private const val SHOWS_AUDIO_SLIDER = "audio slider show"
        private const val EMPTY_STRING = ""

        val instance: AudioGestureConsumer by lazy { AudioGestureConsumer() }

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