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

package com.tunjid.fingergestures.ui.main

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.animation.AnimationUtils.loadAnimation
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.navigation.MultiStackNavigator
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.navigation.multiStackNavigationController
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.baseclasses.BottomSheetNavigator
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.billing.TrialStatus
import com.tunjid.fingergestures.databinding.ActivityMainBinding
import com.tunjid.fingergestures.databinding.TrialViewBinding
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.fragments.AppFragment
import com.tunjid.fingergestures.models.*
import com.tunjid.fingergestures.resultcontracts.PermissionRequestContract
import com.tunjid.fingergestures.resultcontracts.WallpaperPickContract
import com.tunjid.fingergestures.ui.main.AppViewModel

class MainActivity : AppCompatActivity(R.layout.activity_main),
    MainApp,
    GlobalUiHost,
    Navigator.Controller {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val viewModel by viewModelFactory<AppViewModel>()

    private val links get() = (viewModel.state.value?.links ?: listOf()).toTypedArray()

    override val permissionContract = registerForActivityResult(PermissionRequestContract(::getIntent)) {
        lifecycleScope.launchWhenResumed { viewModel.accept(it) }
    }
    override val wallpaperPickContract = registerForActivityResult(WallpaperPickContract(::getIntent)) {
        lifecycleScope.launchWhenResumed {
            it?.let(::cropImage)
                ?: ::uiState.updatePartial { copy(snackbarText = getString(R.string.cancel_wallpaper)) }
        }
    }
    override val cropWallpaperContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Do nothing, result already written to a URI
    }

    override val activity get() = this
    override val inputs get() = viewModel

    override val globalUiController: GlobalUiController by lazy {
        GlobalUiDriver(host = this, binding = binding, navigator = navigator)
    }

    override val bottomSheetNavigator: BottomSheetNavigator by lazy {
        val primary = colorAt(R.color.colorPrimary)
        val (r, g, b) = Color.valueOf(primary)

        BottomSheetNavigator(host = this, binding = binding) { a ->
            bottomNavBackground.colors = intArrayOf(primary, Color.argb((0.5f * a + 0.5f), r, g, b))
            ::uiState.updatePartial { copy(statusBarColor = Color.argb(a, r, g, b)) }
        }
    }
    override val navigator: MultiStackNavigator by multiStackNavigationController(
        stackCount = 5,
        containerId = R.id.content_container
    ) { AppFragment.newInstance(Tab.values()[it]) }

    override var uiState: UiState
        get() = globalUiController.uiState
        set(value) {
            globalUiController.uiState = value
        }

    private val bottomNavBackground by lazy {
        val primary = colorAt(R.color.colorPrimary)
        val (r, g, b) = Color.valueOf(primary)

        GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(primary, Color.argb(0.5f, r, g, b))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        uiState = uiState.copy(
            toolbarShows = true,
            toolbarOverlaps = true,
            toolbarMenuRes = R.menu.activity_main,
            toolbarMenuRefresher = ::refreshToolbarMenu,
            toolbarMenuClickListener = ::onMenuItemSelected,
            toolbarTitle = getString(R.string.app_name),
            showsBottomNav = true,
            insetFlags = InsetFlags.NO_BOTTOM,
            fabClickListener = { viewModel.accept(Input.Permission.Action.Clicked()) },
//            navBarColor = colorAt(
//                if (BackgroundManager.instance.usesColoredNav()) R.color.colorPrimary
//                else R.color.black
//            ),
            navBarColor = colorAt(R.color.colorPrimary),
        )

        // TODO: Back press dispatcher for bottom sheet
        onBackPressedDispatcher.addCallback(this) { if (!navigator.pop()) finish() }

        bottomSheetNavigator.pop()

        binding.bottomNavigation.setOnNavigationItemSelectedListener { onMenuItemSelected(it); true }
        binding.bottomNavigation.setOnApplyWindowInsetsListener { _: View?, windowInsets: WindowInsets? -> windowInsets }
        binding.bottomNavigation.background = bottomNavBackground

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                if (f is AppFragment) viewModel.accept(Input.Permission.Action.Clear())
            }
        }, true)

        navigator.stackSelectedListener = { binding.bottomNavigation.menu.findItem(Tab.values()[it].resource)?.isChecked = true }
        navigator.stackTransactionModifier = {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }

        val startIntent = intent
        val isPickIntent = startIntent != null && ACTION_SEND == startIntent.action

        if (savedInstanceState == null && isPickIntent) handleIntent(startIntent)

        binding.upgradePrompt.apply {
            setFactory {
                val view = layoutInflater.inflate(R.layout.text_switch, this, false)
                view.setOnClickListener { viewModel.accept(Input.Shill) }
                view
            }

            inAnimation = loadAnimation(context, android.R.anim.slide_in_left)
            outAnimation = loadAnimation(context, android.R.anim.slide_out_right)
        }

        viewModel.state
            .mapDistinct(AppState::shilling)
            .observe(this) {
                if (it is Shilling.Quip) binding.upgradePrompt.apply { setText(it.message); isVisible = true }
                else hideAds()
            }
        observe(viewModel.state)
    }

    override fun onResume() {
        super.onResume()
        if (!accessibilityServiceEnabled) viewModel.accept(Input.Permission.Request.Accessibility)
    }

    private fun refreshToolbarMenu(menu: Menu) {
        val trialItem = menu.findItem(R.id.action_start_trial)
        val trialBinding = TrialViewBinding.bind(trialItem.actionView)

        val purchaseState = viewModel.state.value?.purchasesState
        val isTrialRunning = purchaseState?.isOnTrial == true

        if (!isTrialRunning) trialBinding.icon.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(R.string.app_name)
                setMessage(purchaseState?.trialPeriodText ?: "")
                setPositiveButton(android.R.string.yes) { _, _ -> viewModel.accept(Input.StartTrial) }
                show()
            }
        }

        CheatSheet.setup(trialBinding.root, trialItem.title)
        viewModel.state.value?.purchasesState?.let(trialBinding::bind)
    }

    private fun onMenuItemSelected(item: MenuItem) {
        when (val id = item.itemId) {
            R.id.action_directions,
            R.id.action_slider,
            R.id.action_audio,
            R.id.action_accessibility_popup,
            R.id.action_wallpaper -> Tab.values()
                .firstOrNull { it.resource == id }
                ?.ordinal
                ?.let(navigator::show)
            R.id.info -> MaterialAlertDialogBuilder(this)
                .setTitle(R.string.open_source_libraries)
                .setItems(links) { _, index -> showLink(links[index]) }
                .show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (ACTION_SEND != action || type == null || !type.startsWith("image/")) return

        if (!hasStoragePermission) {
            ::uiState.updatePartial { copy(snackbarText = getString(R.string.enable_storage_settings)) }
            navigator.show(Tab.Gestures.ordinal)
            return
        }

        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return

//        navigator.performConsecutively(lifecycleScope) {
//            show(Tab.Display.ordinal)
//            MaterialAlertDialogBuilder(this@MainActivity)
//                .setTitle(R.string.choose_target)
//                .setItems(
//                    WallpaperSelection.values()
//                        .map(WallpaperSelection::textRes)
//                        .map(::getString)
//                        .toTypedArray()
//                ) { _, index ->
//                    val selection = if (index == 0) WallpaperSelection.Day
//                    else WallpaperSelection.Night
//
//                    val shown = navigator.current as AppFragment?
//
//                    if (shown != null && shown.isVisible) shown.doOnLifecycleEvent(Lifecycle.Event.ON_RESUME) { shown.cropImage(imageUri, selection) }
//                    else ::uiState.updatePartial { copy(snackbarText = getString(R.string.error_wallpaper)) }
//                }
//                .show()
//        }
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
                override fun onTransitionEnd(transition: Transition) = ::uiState.updatePartial { copy(snackbarText = getString(R.string.billing_thanks)) }
            })
        })
        binding.upgradePrompt.visibility = View.GONE
    }
}

private fun TrialViewBinding.bind(state: PurchasesManager.State) {
    icon.isVisible = !state.isOnTrial
    text.isVisible = state.isOnTrial

    if (state.trialStatus is TrialStatus.Trial) text.text = state.trialStatus.countDown.toString()
}