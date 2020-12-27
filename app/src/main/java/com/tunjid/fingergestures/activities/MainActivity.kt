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

package com.tunjid.fingergestures.activities

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.animation.AnimationUtils.loadAnimation
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.navigation.MultiStackNavigator
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.navigation.doOnLifecycleEvent
import com.tunjid.androidx.navigation.multiStackNavigationController
import com.tunjid.androidx.uidrivers.UiState
import com.tunjid.androidx.uidrivers.updatePartial
import com.tunjid.androidx.view.util.marginLayoutParams
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.App.Companion.accessibilityServiceEnabled
import com.tunjid.fingergestures.App.Companion.hasStoragePermission
import com.tunjid.fingergestures.BackgroundManager.Companion.ACTION_EDIT_WALLPAPER
import com.tunjid.fingergestures.BackgroundManager.Companion.ACTION_NAV_BAR_CHANGED
import com.tunjid.fingergestures.billing.BillingManager
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.billing.PurchasesManager.Companion.ACTION_LOCKED_CONTENT_CHANGED
import com.tunjid.fingergestures.databinding.ActivityMainBinding
import com.tunjid.fingergestures.fragments.AppFragment
import com.tunjid.fingergestures.models.*
import com.tunjid.fingergestures.services.FingerGestureService.Companion.ACTION_SHOW_SNACK_BAR
import com.tunjid.fingergestures.services.FingerGestureService.Companion.EXTRA_SHOW_SNACK_BAR
import com.tunjid.fingergestures.viewmodels.*
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity(R.layout.activity_main), GlobalUiHost, Navigator.Controller {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(binding.bottomSheet) }

    private var billingManager: BillingManager? = null

    private val disposables = CompositeDisposable()

    private val viewModel by viewModels<AppViewModel>()

    private val links get() = (viewModel.liveState.value?.links ?: listOf()).toTypedArray()

    override val globalUiController: GlobalUiController by lazy { GlobalUiDriver(this, binding, navigator) }

    override val navigator: MultiStackNavigator by multiStackNavigationController(
        stackCount = 5,
        containerId = R.id.content_container
    ) {
        AppFragment.newInstance(Tab.values()[it])
    }

    private var uiState: UiState
        get() = globalUiController.uiState
        set(value) {
            globalUiController.uiState = value
        }

    private val navBarColor: Int
        get() = colorAt(
            if (BackgroundManager.instance.usesColoredNav()) R.color.colorPrimary
            else R.color.black
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected)
        bottomNavigationView.setOnApplyWindowInsetsListener { _: View?, windowInsets: WindowInsets? -> windowInsets }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                if (f is AppFragment) viewModel.accept(Input.Permission.Action.Clear())
            }
        }, true)

        navigator.stackSelectedListener = { bottomNavigationView.menu.findItem(Tab.values()[it].resource)?.isChecked = true }
        navigator.stackTransactionModifier = {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
        onBackPressedDispatcher.addCallback(this) { if (!navigator.pop()) finish() }

        val primary = colorAt(R.color.colorPrimary)
        val (r, g, b) = Color.valueOf(primary)

        val bottomNavBackground = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(primary, Color.argb(0.5f, r, g, b)))

        bottomNavigationView.background = bottomNavBackground

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val a = (slideOffset + 1) / 2 // callback range is [-1, 1]
                bottomNavBackground.colors = intArrayOf(primary, Color.argb((0.5f * a + 0.5f), r, g, b))
                ::uiState.updatePartial { copy(statusBarColor = Color.argb(a, r, g, b)) }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
        })


        binding.bottomSheet.doOnLayout { sheet ->
            globalUiController.liveUiState
                .map { it.systemUI.static.statusBarSize }
                .distinctUntilChanged()
                .observe(this) { sheet.marginLayoutParams.topMargin = it }
        }
        toggleBottomSheet(false)

        val startIntent = intent
        val isPickIntent = startIntent != null && ACTION_SEND == startIntent.action

        if (savedInstanceState == null && isPickIntent) handleIntent(startIntent)

        binding.upgradePrompt.apply {
            setFactory {
                val view = layoutInflater.inflate(R.layout.text_switch, this, false)
                view.setOnClickListener { viewModel.shillMoar() }
                view
            }

            inAnimation = loadAnimation(context, android.R.anim.slide_in_left)
            outAnimation = loadAnimation(context, android.R.anim.slide_out_right)
        }

        viewModel.liveState.apply {
            mapDistinct(AppState::uiUpdate)
                .observe(this@MainActivity, ::onStateChanged)

            mapDistinct(AppState::permissionState)
                .mapDistinct(PermissionState::active)
                .nonNull()
                .map(Unique<Input.Permission.Request>::item)
                .filterUnhandledEvents()
                .observe(this@MainActivity, ::onPermissionClicked)

            mapDistinct(AppState::permissionState)
                .mapDistinct(PermissionState::prompt)
                .nonNull()
                .map(Unique<Int>::item)
                .filterUnhandledEvents()
                .observe(this@MainActivity, ::showSnackbar)

            mapDistinct(AppState::uiInteraction)
                .filterUnhandledEvents()
                .observe(this@MainActivity, ::onUiInteraction)
        }

        viewModel.shill.observe(this) {
            if (it is Shilling.Quip) binding.upgradePrompt.apply { setText(it.message); isVisible = true }
            else hideAds()
        }

        viewModel.broadcasts.observe(this, EventObserver(this::onBroadcastReceived))
    }

    override fun onResume() {
        super.onResume()
        billingManager = BillingManager(applicationContext)
        uiState = uiState.copy(toolbarInvalidated = true)
        if (!accessibilityServiceEnabled) viewModel.accept(Input.Permission.Request.Accessibility)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (val id = item.itemId) {
            R.id.action_start_trial -> {
                val purchasesManager = PurchasesManager.instance
                val isTrialRunning = purchasesManager.isTrialRunning

                withSnackbar { snackbar ->
                    snackbar.setText(viewModel.liveState.value?.purchasesState?.trialPeriodText
                        ?: "")
                    snackbar.duration = if (isTrialRunning) LENGTH_SHORT else LENGTH_INDEFINITE

                    if (!isTrialRunning)
                        snackbar.setAction(android.R.string.yes) { purchasesManager.startTrial(); recreate() }

                    snackbar.show()
                }
            }
            R.id.action_directions,
            R.id.action_slider,
            R.id.action_audio,
            R.id.action_accessibility_popup,
            R.id.action_wallpaper -> {
                Tab.at(id).ordinal.let(navigator::show)
                return true
            }
            R.id.info -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.open_source_libraries)
                    .setItems(links) { _, index -> showLink(links[index]) }
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() =
        if (bottomSheetBehavior.state != STATE_HIDDEN) toggleBottomSheet(false)
        else super.onBackPressed()

    override fun onPause() {
        billingManager?.destroy()
        billingManager = null
        disposables.clear()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        navigator.current?.doOnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
            Input.Permission.Request.forCode(requestCode)
                ?.let(Input.Permission.Action::Changed)
                ?.let(viewModel::accept)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Input.Permission.Request.Storage.code && grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED)
            navigator.current?.doOnLifecycleEvent(Lifecycle.Event.ON_RESUME) {
                Input.Permission.Request.forCode(requestCode)
                    ?.let(Input.Permission.Action::Changed)
                    ?.let(viewModel::accept)
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun toggleBottomSheet(show: Boolean) {
        bottomSheetBehavior.state = if (show) STATE_COLLAPSED else STATE_HIDDEN
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (ACTION_SEND != action || type == null || !type.startsWith("image/")) return

        if (!hasStoragePermission) {
            showSnackbar(R.string.enable_storage_settings)
            navigator.show(Tab.at(R.id.action_directions).ordinal)
            return
        }

        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return

        navigator.performConsecutively(lifecycleScope) {
            show(Tab.at(R.id.action_wallpaper).ordinal)
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.choose_target)
                .setItems(
                    WallpaperSelection.values()
                        .map(WallpaperSelection::textRes)
                        .map(::getString)
                        .toTypedArray()
                ) { _, index ->
                    val selection = if (index == 0) WallpaperSelection.Day
                    else WallpaperSelection.Night

                    val shown = navigator.current as AppFragment?

                    if (shown != null && shown.isVisible) shown.doOnLifecycleEvent(Lifecycle.Event.ON_RESUME) { shown.cropImage(imageUri, selection) }
                    else showSnackbar(R.string.error_wallpaper)
                }
                .show()
        }
    }

    private fun showPermissionDialog(@StringRes stringRes: Int, yesAction: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(stringRes)
            .setPositiveButton(R.string.yes) { _, _ -> yesAction.invoke() }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun onPermissionClicked(permissionRequest: Input.Permission.Request) =
        when (permissionRequest) {
            Input.Permission.Request.DoNotDisturb -> showPermissionDialog(R.string.do_not_disturb_permissions_request) { startActivityForResult(App.doNotDisturbIntent, Input.Permission.Request.DoNotDisturb.code) }
            Input.Permission.Request.Accessibility -> showPermissionDialog(R.string.accessibility_permissions_request) { startActivityForResult(App.accessibilityIntent, Input.Permission.Request.Accessibility.code) }
            Input.Permission.Request.Settings -> showPermissionDialog(R.string.settings_permission_request) { startActivityForResult(App.settingsIntent, Input.Permission.Request.Settings.code) }
            Input.Permission.Request.Storage -> showPermissionDialog(R.string.wallpaper_permission_request) { requestPermissions(STORAGE_PERMISSIONS, Input.Permission.Request.Storage.code) }
        }

    private fun onUiInteraction(it: Input.UiInteraction) {
        when (it) {
            is Input.UiInteraction.ShowSheet -> {
                supportFragmentManager.beginTransaction().replace(R.id.bottom_sheet, it.fragment).commit()
                toggleBottomSheet(true)
            }
            is Input.UiInteraction.GoPremium -> MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.go_premium_title)
                .setMessage(getString(R.string.go_premium_body, getString(it.description)))
                .setPositiveButton(R.string.continue_text) { _, _ -> purchase(PurchasesManager.Sku.Premium) }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
            is Input.UiInteraction.Purchase -> purchase(it.sku)
            is Input.UiInteraction.WallpaperPick -> when {
                hasStoragePermission -> startActivityForResult(Intent.createChooser(Intent()
                    .setType(IMAGE_SELECTION)
                    .setAction(Intent.ACTION_GET_CONTENT), ""), it.selection.code)
                else -> ::uiState.updatePartial { copy(snackbarText = getString(R.string.enable_storage_settings)) }
            }
        }
    }

    private fun showLink(textLink: TextLink) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link))
        startActivity(browserIntent)
    }

    private fun hideAds() {
        if (binding.upgradePrompt.visibility == View.GONE) return

        TransitionManager.beginDelayedTransition(binding.root, AutoTransition().apply {
            addTarget(binding.upgradePrompt)
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition) = showSnackbar(R.string.billing_thanks)
            })
        })
        binding.upgradePrompt.visibility = View.GONE
    }

    private fun onStateChanged(uiUpdate: UiUpdate) {
        uiState = uiState.copy(
            fabIcon = uiUpdate.iconRes,
            fabText = getString(uiUpdate.titleRes),
            fabShows = uiUpdate.fabVisible
        )
    }

    private fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            ACTION_EDIT_WALLPAPER -> showSnackbar(R.string.error_wallpaper_google_photos)
            ACTION_SHOW_SNACK_BAR -> showSnackbar(intent.getIntExtra(EXTRA_SHOW_SNACK_BAR, R.string.generic_error))
            ACTION_NAV_BAR_CHANGED -> window.navigationBarColor = navBarColor
            ACTION_LOCKED_CONTENT_CHANGED -> recreate()
        }
    }

    fun showSnackbar(@StringRes resource: Int) = globalUiController::uiState.updatePartial {
        copy(snackbarText = getText(resource))
    }

    fun purchase(sku: PurchasesManager.Sku) = when (val billingManager = billingManager) {
        null -> showSnackbar(R.string.generic_error)
        else -> disposables.add(billingManager.initiatePurchaseFlow(this, sku)
            .subscribe({ launchStatus ->
                when (launchStatus) {
                    BillingClient.BillingResponse.OK -> Unit
                    BillingClient.BillingResponse.SERVICE_UNAVAILABLE, BillingClient.BillingResponse.SERVICE_DISCONNECTED -> showSnackbar(R.string.billing_not_connected)
                    BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> showSnackbar(R.string.billing_you_own_this)
                    else -> showSnackbar(R.string.generic_error)
                }
            }, { showSnackbar(R.string.generic_error) })).let { }
    }

    private fun withSnackbar(consumer: (Snackbar) -> Unit) {
        val snackbar = Snackbar.make(binding.root, R.string.app_name, Snackbar.LENGTH_SHORT)
        snackbar.view.setOnApplyWindowInsetsListener { _, insets -> insets }
        consumer.invoke(snackbar)
    }

}

private const val IMAGE_SELECTION = "image/*"

private val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
