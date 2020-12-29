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
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.*
import com.tunjid.fingergestures.models.Broadcast
import dagger.Module
import dagger.Provides
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
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

    @Provides
    @Singleton
    fun provideDagger(): Dagger = app.dagger

    @Provides
    @AppContext
    fun provideAppContext(): Context = app

    @Provides
    @Singleton
    fun providePurchasesManager(): PurchasesManager =
        PurchasesManager()

    @Provides
    @Singleton
    fun provideAppBroadcasts(): AppBroadcasts = broadcasts

    @Provides
    @Singleton
    fun provideAppBroadcaster(): AppBroadcaster = broadcaster

    @Provides
    @Singleton
    fun provideBackgroundManager(): BackgroundManager =
        BackgroundManager(
            app = app,
            broadcaster = broadcaster,
        )

    @Provides
    @Singleton
    fun provideNothingGestureConsumer(): NothingGestureConsumer =
        NothingGestureConsumer()

    @Provides
    @Singleton
    fun provideBrightnessGestureConsumer(purchasesManager: PurchasesManager): BrightnessGestureConsumer =
        BrightnessGestureConsumer(
            app = app,
            broadcaster = broadcaster,
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
    fun provideRotationGestureConsumer(purchasesManager: PurchasesManager): RotationGestureConsumer =
        RotationGestureConsumer(
            app = app,
            broadcaster = broadcaster,
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
        purchasesManager: PurchasesManager
    ): PopUpGestureConsumer =
        PopUpGestureConsumer(
            context = app,
            broadcaster = broadcaster,
            purchasesManager = purchasesManager
        )

    @Provides
    @Singleton
    fun provideAudioGestureConsumer(): AudioGestureConsumer =
        AudioGestureConsumer(broadcaster = broadcaster)

    @Provides
    @Singleton
    fun provideGestureMapper(
        purchasesManager: PurchasesManager,
        consumers: GestureConsumers
    ): GestureMapper =
        GestureMapper(
            purchasesManager = purchasesManager,
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