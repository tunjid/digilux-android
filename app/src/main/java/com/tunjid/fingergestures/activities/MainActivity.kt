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
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.TextSwitcher
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment
import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator
import com.tunjid.androidbootstrap.view.animator.ViewHider
import com.tunjid.androidbootstrap.view.util.ViewUtil.getLayoutParams
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.App.Companion.accessibilityServiceEnabled
import com.tunjid.fingergestures.App.Companion.hasStoragePermission
import com.tunjid.fingergestures.App.Companion.withApp
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.BackgroundManager.ACTION_EDIT_WALLPAPER
import com.tunjid.fingergestures.BackgroundManager.ACTION_NAV_BAR_CHANGED
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.TrialView
import com.tunjid.fingergestures.baseclasses.FingerGestureActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.billing.PurchasesManager.ACTION_LOCKED_CONTENT_CHANGED
import com.tunjid.fingergestures.fragments.AppFragment
import com.tunjid.fingergestures.models.TextLink
import com.tunjid.fingergestures.models.UiState
import com.tunjid.fingergestures.services.FingerGestureService.ACTION_SHOW_SNACK_BAR
import com.tunjid.fingergestures.services.FingerGestureService.EXTRA_SHOW_SNACK_BAR
import com.tunjid.fingergestures.viewholders.DiffViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class MainActivity : FingerGestureActivity() {

    private var insetsApplied: Boolean = false

    private lateinit var topInsetView: View
    private lateinit var bottomInsetView: View
    private lateinit var toolbar: Toolbar
    private lateinit var constraintLayout: ViewGroup
    private lateinit var shillSwitcher: TextSwitcher
    private lateinit var permissionText: MaterialButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private lateinit var fabExtensionAnimator: FabExtensionAnimator
    private lateinit var disposables: CompositeDisposable

    private lateinit var viewModel: AppViewModel

    private val navBarColor: Int
        get() = ContextCompat.getColor(this, if (BackgroundManager.getInstance().usesColoredNav())
            R.color.colorPrimary
        else
            R.color.black)

    private val fabTint: ColorStateList
        get() = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary))

    private val transition: Transition
        get() = AutoTransition().excludeTarget(RecyclerView::class.java, true)

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(STORAGE_CODE, SETTINGS_CODE, ACCESSIBILITY_CODE, DO_NOT_DISTURB_CODE)
    annotation class PermissionRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(AppViewModel::class.java)
        disposables = CompositeDisposable()

        toolbar = findViewById(R.id.toolbar)
        topInsetView = findViewById(R.id.top_inset)
        bottomInsetView = findViewById(R.id.bottom_inset)
        shillSwitcher = findViewById(R.id.upgrade_prompt)
        permissionText = findViewById(R.id.permission_view)
        constraintLayout = findViewById(R.id.constraint_layout)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        fabHider = ViewHider.of(permissionText).setDirection(ViewHider.BOTTOM).build()
        barHider = ViewHider.of(toolbar).setDirection(ViewHider.TOP).build()
        fabExtensionAnimator = FabExtensionAnimator(permissionText)

        fabHider.hide()

        permissionText.backgroundTintList = fabTint
        permissionText.setOnClickListener { viewModel.onPermissionClicked(this::onPermissionClicked) }
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onOptionsItemSelected)
        constraintLayout.setOnApplyWindowInsetsListener { _, insets -> consumeSystemInsets(insets) }

        val window = window
        window.decorView.systemUiVisibility = DEFAULT_SYSTEM_UI_FLAGS
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorBackground)
        window.navigationBarColor = navBarColor

        setSupportActionBar(toolbar)
        toggleBottomSheet(false)

        val startIntent = intent
        val isPickIntent = startIntent != null && ACTION_SEND == startIntent.action

        if (savedInstanceState == null && isPickIntent)
            handleIntent(startIntent)
        else if (savedInstanceState == null) showAppFragment(viewModel.gestureItems)

        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager,
                                               f: Fragment,
                                               v: View,
                                               savedInstanceState: Bundle?) {
                if (f is AppFragment)
                    updateBottomNav(f, bottomNavigationView)
            }
        }, false)

        setUpSwitcher()
    }

    override fun onResume() {
        super.onResume()

        if (PurchasesManager.getInstance().hasAds())
            shill()
        else
            hideAds()

        if (!accessibilityServiceEnabled()) requestPermission(ACCESSIBILITY_CODE)

        invalidateOptionsMenu()
        subscribeToBroadcasts()
        disposables.add(viewModel.uiState().subscribe(this::onStateChanged, Throwable::printStackTrace))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_start_trial)
        val isTrialVisible = !PurchasesManager.getInstance().isPremiumNotTrial

        if (item != null) item.isVisible = isTrialVisible
        if (isTrialVisible && item != null) item.actionView = TrialView(this, item)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_start_trial -> {
                val purchasesManager = PurchasesManager.getInstance()
                val isTrialRunning = purchasesManager.isTrialRunning

                withSnackbar { snackbar ->
                    snackbar.setText(purchasesManager.trialPeriodText)
                    snackbar.duration = if (isTrialRunning) LENGTH_SHORT else LENGTH_INDEFINITE

                    if (!isTrialRunning)
                        snackbar.setAction(android.R.string.yes) { purchasesManager.startTrial();recreate() }

                    snackbar.show()
                }
            }
            R.id.action_directions -> {
                showAppFragment(viewModel.gestureItems)
                return true
            }
            R.id.action_slider -> {
                showAppFragment(viewModel.brightnessItems)
                return true
            }
            R.id.action_audio -> {
                showAppFragment(viewModel.audioItems)
                return true
            }
            R.id.action_accessibility_popup -> {
                showAppFragment(viewModel.popupItems)
                return true
            }
            R.id.action_wallpaper -> {
                showAppFragment(viewModel.appearanceItems)
                return true
            }
            R.id.info -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.open_source_libraries)
                        .setItems(viewModel.state.links) { _, index -> showLink(viewModel.state.links[index]) }
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
        disposables.clear()
        super.onPause()
    }

    override fun onDestroy() {
        DiffViewHolder.onActivityDestroyed()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onPermissionChange(requestCode).ifPresent(this::showSnackbar)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != STORAGE_CODE || grantResults.isEmpty() || grantResults[0] != PERMISSION_GRANTED)
            return

        viewModel.onPermissionChange(requestCode).ifPresent(this::showSnackbar)
        currentFragment?.notifyDataSetChanged()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun getCurrentFragment(): AppFragment? = super.getCurrentFragment() as? AppFragment

    override fun showFragment(fragment: BaseFragment): Boolean {
        viewModel.checkPermissions()
        return super.showFragment(fragment)
    }

    fun requestPermission(@PermissionRequest permission: Int) {
        viewModel.requestPermission(permission)
    }

    fun toggleBottomSheet(show: Boolean) {
        bottomSheetBehavior.state = if (show) STATE_COLLAPSED else STATE_HIDDEN
    }

    private fun showAppFragment(items: IntArray) {
        showFragment(AppFragment.newInstance(items))
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (ACTION_SEND != action || type == null || !type.startsWith("image/")) return

        if (!hasStoragePermission) {
            showSnackbar(R.string.enable_storage_settings)
            showAppFragment(viewModel.gestureItems)
            return
        }

        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return

        val toShow = AppFragment.newInstance(viewModel.appearanceItems)
        val tag = toShow.stableTag

        showFragment(toShow)

        BackgroundManager.getInstance().requestWallPaperConstant(R.string.choose_target, this) { selection ->
            val shown = supportFragmentManager.findFragmentByTag(tag) as AppFragment?
            if (shown != null && shown.isVisible)
                shown.cropImage(imageUri, selection)
            else
                showSnackbar(R.string.error_wallpaper)
        }
    }

    private fun askForStorage() {
        showPermissionDialog(R.string.wallpaper_permission_request) { requestPermissions(STORAGE_PERMISSIONS, STORAGE_CODE) }
    }

    private fun askForSettings() {
        showPermissionDialog(R.string.settings_permission_request) { startActivityForResult(App.settingsIntent, SETTINGS_CODE) }
    }

    private fun askForAccessibility() {
        showPermissionDialog(R.string.accessibility_permissions_request) { startActivityForResult(App.accessibilityIntent, ACCESSIBILITY_CODE) }
    }

    private fun askForDoNotDisturb() {
        showPermissionDialog(R.string.do_not_disturb_permissions_request) { startActivityForResult(App.doNotDisturbIntent, DO_NOT_DISTURB_CODE) }
    }

    private fun showPermissionDialog(@StringRes stringRes: Int, yesAction: () -> Unit) {
        AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(stringRes)
                .setPositiveButton(R.string.yes) { _, _ -> yesAction.invoke() }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun onPermissionClicked(permissionRequest: Int) {
        when (permissionRequest) {
            DO_NOT_DISTURB_CODE -> askForDoNotDisturb()
            ACCESSIBILITY_CODE -> askForAccessibility()
            SETTINGS_CODE -> askForSettings()
            STORAGE_CODE -> askForStorage()
        }
    }

    private fun updateBottomNav(fragment: AppFragment, bottomNavigationView: BottomNavigationView) =
            viewModel.updateBottomNav(Arrays.hashCode(fragment.items)).ifPresent { bottomNavigationView.selectedItemId = it }

    private fun showLink(textLink: TextLink) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(textLink.link))
        startActivity(browserIntent)
    }

    private fun shill() {
        disposables.add(viewModel.shill().subscribe(shillSwitcher::setText, Throwable::printStackTrace))
    }

    private fun hideAds() {
        viewModel.calmIt()
        if (shillSwitcher.visibility == View.GONE) return

        val hideTransition = transition
        hideTransition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                showSnackbar(R.string.billing_thanks)
            }
        })
        TransitionManager.beginDelayedTransition(constraintLayout, hideTransition)
        shillSwitcher.visibility = View.GONE
    }

    private fun setUpSwitcher() {
        shillSwitcher.setFactory {
            val view = LayoutInflater.from(this).inflate(R.layout.text_switch, shillSwitcher, false)
            view.setOnClickListener { viewModel.shillMoar() }
            view
        }

        shillSwitcher.inAnimation = loadAnimation(this, android.R.anim.slide_in_left)
        shillSwitcher.outAnimation = loadAnimation(this, android.R.anim.slide_out_right)
    }

    private fun onStateChanged(uiState: UiState) {
        permissionText.post { fabExtensionAnimator.updateGlyphs(uiState.glyphState) }
        permissionText.post(if (uiState.fabVisible) Runnable { fabHider.show() } else Runnable { fabHider.hide() })
    }

    private fun subscribeToBroadcasts() {
        withApp { app ->
            disposables.add(app.broadcasts()
                    .filter { this.intentMatches(it) }
                    .subscribe({ this.onBroadcastReceived(it) }, { error ->
                        error.printStackTrace()
                        subscribeToBroadcasts() // Resubscribe on error
                    }))
        }
    }

    private fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            ACTION_EDIT_WALLPAPER -> showSnackbar(R.string.error_wallpaper_google_photos)
            ACTION_SHOW_SNACK_BAR -> showSnackbar(intent.getIntExtra(EXTRA_SHOW_SNACK_BAR, R.string.generic_error))
            ACTION_NAV_BAR_CHANGED -> window.navigationBarColor = navBarColor
            ACTION_LOCKED_CONTENT_CHANGED -> recreate()
        }
    }

    private fun intentMatches(intent: Intent): Boolean {
        val action = intent.action
        return (ACTION_EDIT_WALLPAPER == action
                || ACTION_SHOW_SNACK_BAR == action
                || ACTION_NAV_BAR_CHANGED == action
                || ACTION_LOCKED_CONTENT_CHANGED == action)
    }

    private fun consumeSystemInsets(insets: WindowInsets): WindowInsets {
        if (insetsApplied) return insets

        topInset = insets.systemWindowInsetTop
        val leftInset = insets.systemWindowInsetLeft
        val rightInset = insets.systemWindowInsetRight
        bottomInset = insets.systemWindowInsetBottom

        getLayoutParams(toolbar).topMargin = topInset
        getLayoutParams(topInsetView).height = topInset
        getLayoutParams(bottomInsetView).height = bottomInset
        constraintLayout.setPadding(leftInset, 0, rightInset, 0)

        insetsApplied = true
        return insets
    }

    companion object {

        var topInset: Int = 0
        var bottomInset: Int = 0

        const val STORAGE_CODE = 100
        const val SETTINGS_CODE = 200
        const val ACCESSIBILITY_CODE = 300
        const val DO_NOT_DISTURB_CODE = 400

        private const val DEFAULT_SYSTEM_UI_FLAGS = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        private val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
