package com.tunjid.fingergestures.models

import android.os.Build
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.core.view.*
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.tunjid.androidx.core.content.drawableAt
import com.tunjid.androidx.material.animator.FabExtensionAnimator
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.view.animator.ViewHider
import com.tunjid.androidx.view.util.*
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.databinding.ActivityMainBinding
import com.tunjid.fingergestures.map
import com.tunjid.fingergestures.ui.main.Shilling
import kotlin.math.max

/**
 * An interface for classes that host a [UiState], usually a [FragmentActivity].
 * Implementations should delegate to an instance of [GlobalUiDriver]
 */

interface GlobalUiHost {
    val globalUiController: GlobalUiController
}

interface GlobalUiController {
    var uiState: UiState
    val liveUiState: LiveData<UiState>
}

/**
 * Drives global UI that is common from screen to screen described by a [UiState].
 * This makes it so that these persistent UI elements aren't duplicated, and only animate themselves when they change.
 * This is the default implementation of [GlobalUiController] that other implementations of
 * the same interface should delegate to.
 */
class GlobalUiDriver(
    private val host: FragmentActivity,
    private val binding: ActivityMainBinding,
    private val navigator: Navigator
) : GlobalUiController {

    private val snackbar = Snackbar.make(binding.contentRoot, "", Snackbar.LENGTH_SHORT).apply {
        view.setOnApplyWindowInsetsListener(noOpInsetsListener)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            ::uiState.updatePartial {
                copy(systemUI = systemUI.updateSnackbarHeight(view.height))
            }
        }
        addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                when (event) {
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_SWIPE,
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION,
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT,
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL -> ::uiState.updatePartial {
                        copy(snackbarText = "", systemUI = systemUI.updateSnackbarHeight(0))
                    }
                    BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_CONSECUTIVE -> Unit
                }
            }
        })
    }

    private val shortestAvailableLifecycle
        get() = when (val current = navigator.current) {
            null -> host.lifecycle
            else -> if (current.view == null) current.lifecycle else current.viewLifecycleOwner.lifecycle
        }

    private val uiSizes = UISizes(host)
    private val fabExtensionAnimator = FabExtensionAnimator(binding.fab)
    private val toolbarHider = ViewHider.of(binding.toolbar).setDirection(ViewHider.TOP).build()
    private val noOpInsetsListener = View.OnApplyWindowInsetsListener { _, insets -> insets }
    private val rootInsetsListener = View.OnApplyWindowInsetsListener { _, insets ->
        liveUiState.value = uiState.reduceSystemInsets(insets, uiSizes.navBarHeightThreshold)
        // Consume insets so other views will not see them.
        insets.consumeSystemWindowInsets()
    }
    override val liveUiState = MutableLiveData<UiState>()

    override var uiState: UiState
        get() = liveUiState.value ?: UiState()
        set(value) {
            val updated = value.copy(
                systemUI = value.systemUI.filterNoOp(uiState.systemUI),
                fabClickListener = value.fabClickListener.lifecycleAware(),
                fabTransitionOptions = value.fabTransitionOptions.lifecycleAware(),
                toolbarMenuRefresher = value.toolbarMenuRefresher.lifecycleAware(),
                toolbarMenuClickListener = value.toolbarMenuClickListener.lifecycleAware()
            )
            liveUiState.value = updated
            liveUiState.value = updated.copy(toolbarInvalidated = false) // Reset after firing once
        }

    init {
        host.window.decorView.systemUiVisibility = FULL_CONTROL_SYSTEM_UI_FLAGS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) host.window.isNavigationBarContrastEnforced = false

        binding.root.setOnApplyWindowInsetsListener(rootInsetsListener)

        binding.toolbar.setNavigationOnClickListener { navigator.pop() }
        binding.toolbar.setOnApplyWindowInsetsListener(noOpInsetsListener)

        binding.bottomNavigation.doOnLayout { updateBottomNav(this@GlobalUiDriver.uiState.bottomNavPositionalState) }
        binding.bottomNavigation.setOnApplyWindowInsetsListener(noOpInsetsListener)

        binding.contentContainer.setOnApplyWindowInsetsListener(noOpInsetsListener)
        binding.contentContainer.spring(PaddingProperty.BOTTOM).apply {
            // Scroll to text that has focus
            addEndListener { _, _, _, _ -> (binding.contentContainer.innermostFocusedChild as? EditText)?.let { it.text = it.text } }
        }

        UiState::toolbarShows.distinct onChanged toolbarHider::set
        UiState::toolbarState.distinct onChanged toolbarHider.view::updatePartial
        UiState::toolbarMenuClickListener.distinct onChanged this::setMenuItemClickListener
        UiState::toolbarPosition.distinct onChanged { binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = it } }

        UiState::fabGlyphs.distinct onChanged this::setFabGlyphs
        UiState::fabState.distinct onChanged this::updateFabState
        UiState::fabClickListener.distinct onChanged this::setFabClickListener
        UiState::fabExtended.distinct onChanged fabExtensionAnimator::isExtended::set
        UiState::fabTransitionOptions.distinct onChanged this::setFabTransitionOptions

        UiState::snackbarText.distinct onChanged this::showSnackBar
        UiState::navBarColor.distinct onChanged host.window::setNavigationBarColor
        UiState::statusBarColor.distinct onChanged host.window::setStatusBarColor
        UiState::fragmentContainerState.distinct onChanged this::updateFragmentContainer

        UiState::bottomNavPositionalState.distinct onChanged this::updateBottomNav
        UiState::snackbarPositionalState.distinct onChanged this::updateSnackbar
    }

    private fun updateFabState(state: FabPositionalState) {
        val isShilling = when (val shill = state.shilling) {
            Shilling.Calm -> false
            is Shilling.Quip -> binding.upgradePrompt.setText(shill.message).let { true }
        }

        if (state.fabVisible) binding.fab.isVisible = true
        if (isShilling) binding.upgradePrompt.isVisible = true

        val navBarHeight = state.navBarSize
        val snackbarHeight = state.snackbarHeight + uiSizes.snackbarPadding.countIf(state.snackbarHeight != 0)
        val bottomNavHeight = uiSizes.bottomNavSize countIf state.bottomNavVisible
        val insetClearance = max(bottomNavHeight, state.keyboardSize)
        val shillClearance = uiSizes.shillTextSize countIf isShilling

        val fabTranslation = when {
            state.fabVisible -> -(navBarHeight + insetClearance + snackbarHeight + shillClearance).toFloat()
            else -> binding.fab.height.toFloat() + binding.fab.paddingBottom
        }
        val shillTranslation = when {
            isShilling -> -(navBarHeight + insetClearance + snackbarHeight).toFloat()
            else -> binding.upgradePrompt.height.toFloat()
        }

        binding.fab.softSpring(SpringAnimation.TRANSLATION_Y)
            .withOneShotEndListener { binding.fab.isVisible = state.fabVisible } // Make the fab gone if hidden
            .animateToFinalPosition(fabTranslation)

        binding.upgradePrompt.softSpring(SpringAnimation.TRANSLATION_Y)
            .withOneShotEndListener { binding.upgradePrompt.isVisible = isShilling } // Make the fab gone if hidden
            .animateToFinalPosition(shillTranslation)
    }

    private fun updateSnackbar(state: SnackbarPositionalState) {
        snackbar.view.doOnLayout {
            val bottomNavClearance = uiSizes.bottomNavSize countIf state.bottomNavVisible
            val navBarClearance = state.navBarSize countIf state.insetDescriptor.hasBottomInset
            val insetClearance =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) max(bottomNavClearance, state.keyboardSize)
                else max(bottomNavClearance + navBarClearance, state.keyboardSize)

            it.softSpring(SpringAnimation.TRANSLATION_Y)
                .animateToFinalPosition(-insetClearance.toFloat())
        }
    }

    private fun updateBottomNav(state: BottomNavPositionalState) {
        binding.bottomNavigation.updatePadding(bottom = state.navBarSize)
        binding.bottomNavigation.softSpring(SpringAnimation.TRANSLATION_Y)
            .animateToFinalPosition(if (state.bottomNavVisible) 0f else uiSizes.bottomNavSize.toFloat() + state.navBarSize)
    }

    private fun updateFragmentContainer(state: FragmentContainerPositionalState) {
        val bottomNavHeight = uiSizes.bottomNavSize countIf state.bottomNavVisible
        val insetClearance = max(bottomNavHeight, state.keyboardSize)
        val navBarClearance = state.navBarSize countIf state.insetDescriptor.hasBottomInset
        val totalBottomClearance = insetClearance + navBarClearance

        val statusBarSize = state.statusBarSize countIf state.insetDescriptor.hasTopInset
        val toolbarHeight = uiSizes.toolbarSize countIf !state.toolbarOverlaps
        val topClearance = statusBarSize + toolbarHeight

        binding.contentContainer
            .softSpring(PaddingProperty.TOP)
            .animateToFinalPosition(topClearance.toFloat())

        binding.contentContainer
            .softSpring(PaddingProperty.BOTTOM)
            .animateToFinalPosition(totalBottomClearance.toFloat())
    }

    private fun setMenuItemClickListener(item: ((MenuItem) -> Unit)?) =
        binding.toolbar.setOnMenuItemClickListener {
            item?.invoke(it)?.let { true } ?: host.onOptionsItemSelected(it)
        }

    private fun setFabGlyphs(fabGlyphState: Pair<Int, CharSequence>) = host.runOnUiThread {
        val (@DrawableRes icon: Int, title: CharSequence) = fabGlyphState
        fabExtensionAnimator.updateGlyphs(title, if (icon != 0) host.drawableAt(icon) else null)
    }

    private fun setFabClickListener(onClickListener: ((View) -> Unit)?) =
        binding.fab.setOnClickListener(onClickListener)

    private fun setFabTransitionOptions(options: (SpringAnimation.() -> Unit)?) {
        options?.let(fabExtensionAnimator::configureSpring)
    }

    private fun showSnackBar(message: CharSequence) = if (message.isNotBlank()) {
        snackbar.setText(message)
        snackbar.show()
    } else Unit

    companion object {
        private const val FULL_CONTROL_SYSTEM_UI_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
    }

    /**
     * Maps slices of the ui state to the function that should be invoked when it changes
     */
    private infix fun <T> LiveData<T>.onChanged(consumer: (T) -> Unit) {
        distinctUntilChanged().observe(host, consumer)
    }

    private val <T : Any?> ((UiState) -> T).distinct get() = liveUiState.map(this).distinctUntilChanged()

    /**
     * Wraps an action with the shortest available lifecycle to make sure nothing leaks.
     * If [this] is already a [LifeCycleAwareCallback], it was previously wrapped and will NO-OP.
     */
    private fun <T> ((T) -> Unit).lifecycleAware(): (T) -> Unit =
        if (this is LifeCycleAwareCallback) this else LifeCycleAwareCallback(shortestAvailableLifecycle, this)
}

fun View.softSpring(property: FloatPropertyCompat<View>) =
    spring(property, SpringForce.STIFFNESS_LOW)

private fun ViewHider<*>.set(show: Boolean) =
    if (show) show()
    else hide()

private class LifeCycleAwareCallback<T>(lifecycle: Lifecycle, implementation: (T) -> Unit) : (T) -> Unit {
    private var callback: (T) -> Unit = implementation

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) callback = {}
        })
    }

    override fun invoke(type: T) = callback.invoke(type)
}

private class UISizes(host: FragmentActivity) {
    val toolbarSize: Int = host.resources.getDimensionPixelSize(R.dimen.triple_and_half_margin)
    val bottomNavSize: Int = host.resources.getDimensionPixelSize(R.dimen.triple_and_half_margin)
    val shillTextSize: Int = host.resources.getDimensionPixelSize(R.dimen.triple_margin)
    val snackbarPadding: Int = host.resources.getDimensionPixelSize(R.dimen.half_margin)
    val navBarHeightThreshold: Int = host.resources.getDimensionPixelSize(R.dimen.quintuple_margin)
}

private infix fun Int.countIf(condition: Boolean) = if (condition) this else 0
