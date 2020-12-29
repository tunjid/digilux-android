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

package com.tunjid.fingergestures.di

import android.content.Context
import android.content.SharedPreferences
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.*
import com.tunjid.fingergestures.models.Broadcast
import dagger.Module
import dagger.Provides
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Flowables
import javax.inject.Qualifier
import javax.inject.Singleton


typealias AppBroadcaster = (Broadcast) -> Unit
typealias AppBroadcasts = Flowable<Broadcast>

@Qualifier
annotation class AppContext

@Module
class AppModule(private val app: App) {

    private val backingBroadcaster = PublishProcessor.create<Broadcast>()
    private val broadcasts = backingBroadcaster
    private val broadcaster = backingBroadcaster::onNext

    private val listeners: MutableSet<SharedPreferences.OnSharedPreferenceChangeListener> = mutableSetOf()

    @Provides
    @Singleton
    fun provideDagger(): Dagger = app.dagger

    @Provides
    @AppContext
    fun provideAppContext(): Context = app

    @Provides
    @Singleton
    fun provideReactivePreferences(): ReactivePreferences =
        ReactivePreferences(
            preferences = app.preferences,
            monitor = Flowables.create(BackpressureStrategy.BUFFER) { emitter ->
                val prefs = app.preferences
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    emitter.onNext(key)
                }
                listener
                    .also(prefs::registerOnSharedPreferenceChangeListener)
                    .let(listeners::add)
                emitter.setCancellable {
                    listener
                        .also(prefs::unregisterOnSharedPreferenceChangeListener)
                        .let(listeners::remove)
                }
            }
        )

    @Provides
    @Singleton
    fun providePurchasesManager(reactivePreferences: ReactivePreferences): PurchasesManager =
        PurchasesManager(
            context = app,
            reactivePreferences = reactivePreferences,
        )

    @Provides
    @Singleton
    fun provideAppBroadcasts(): AppBroadcasts = broadcasts

    @Provides
    @Singleton
    fun provideAppBroadcaster(): AppBroadcaster = broadcaster

    @Provides
    @Singleton
    fun provideBackgroundManager(reactivePreferences: ReactivePreferences): BackgroundManager =
        BackgroundManager(
            context = app,
            reactivePreferences = reactivePreferences,
            broadcaster = broadcaster,
        )

    @Provides
    @Singleton
    fun provideNothingGestureConsumer(): NothingGestureConsumer =
        NothingGestureConsumer()

    @Provides
    @Singleton
    fun provideBrightnessGestureConsumer(
        purchasesManager: PurchasesManager,
        reactivePreferences: ReactivePreferences
    ): BrightnessGestureConsumer =
        BrightnessGestureConsumer(
            context = app,
            broadcaster = broadcaster,
            reactivePreferences = reactivePreferences,
            purchasesManager = purchasesManager
        )

    @Provides
    @Singleton
    fun provideNotificationGestureConsumer(): NotificationGestureConsumer =
        NotificationGestureConsumer(
            broadcaster = broadcaster,
        )

    @Provides
    @Singleton
    fun provideFlashlightGestureConsumer(): FlashlightGestureConsumer =
        FlashlightGestureConsumer(app)

    @Provides
    @Singleton
    fun provideDockingGestureConsumer(): DockingGestureConsumer =
        DockingGestureConsumer(app)

    @Provides
    @Singleton
    fun provideRotationGestureConsumer(
        purchasesManager: PurchasesManager,
        reactivePreferences: ReactivePreferences
    ): RotationGestureConsumer =
        RotationGestureConsumer(
            context = app,
            broadcaster = broadcaster,
            reactivePreferences = reactivePreferences,
            purchasesManager = purchasesManager
        )

    @Provides
    @Singleton
    fun provideGlobalActionGestureConsumer(): GlobalActionGestureConsumer =
        GlobalActionGestureConsumer(
            broadcaster = broadcaster,
        )

    @Provides
    @Singleton
    fun providePopUpGestureConsumer(
        purchasesManager: PurchasesManager,
        reactivePreferences: ReactivePreferences
    ): PopUpGestureConsumer =
        PopUpGestureConsumer(
            context = app,
            broadcaster = broadcaster,
            reactivePreferences = reactivePreferences,
            purchasesManager = purchasesManager
        )

    @Provides
    @Singleton
    fun provideAudioGestureConsumer(
        reactivePreferences: ReactivePreferences
    ): AudioGestureConsumer =
        AudioGestureConsumer(
            context = app,
            reactivePreferences = reactivePreferences,
            broadcaster = broadcaster
        )

    @Provides
    @Singleton
    fun provideGestureMapper(
        reactivePreferences: ReactivePreferences,
        purchasesManager: PurchasesManager,
        consumers: GestureConsumers
    ): GestureMapper =
        GestureMapper(
            context = app,
            purchasesManager = purchasesManager,
            reactivePreferences = reactivePreferences,
            consumers = consumers,
            broadcasts = broadcasts,
        )

    @Provides
    @Singleton
    fun provideGestureConsumers(
        nothingGestureConsumer: NothingGestureConsumer,
        brightnessGestureConsumer: BrightnessGestureConsumer,
        notificationGestureConsumer: NotificationGestureConsumer,
        flashlightGestureConsumer: FlashlightGestureConsumer,
        dockingGestureConsumer: DockingGestureConsumer,
        rotationGestureConsumer: RotationGestureConsumer,
        globalActionGestureConsumer: GlobalActionGestureConsumer,
        popUpGestureConsumer: PopUpGestureConsumer,
        audioGestureConsumer: AudioGestureConsumer,
    ): GestureConsumers = GestureConsumers(
        nothing = nothingGestureConsumer,
        brightness = brightnessGestureConsumer,
        notification = notificationGestureConsumer,
        flashlight = flashlightGestureConsumer,
        docking = dockingGestureConsumer,
        rotation = rotationGestureConsumer,
        globalAction = globalActionGestureConsumer,
        popUp = popUpGestureConsumer,
        audio = audioGestureConsumer
    )

    @Provides
    @Singleton
    fun provideAppDependencies(
        purchasesManager: PurchasesManager,
        backgroundManager: BackgroundManager,
        gestureConsumers: GestureConsumers,
        gestureMapper: GestureMapper,
    ): AppDependencies = AppDependencies(
        backgroundManager = backgroundManager,
        purchasesManager = purchasesManager,
        gestureMapper = gestureMapper,
        gestureConsumers = gestureConsumers
    )
}