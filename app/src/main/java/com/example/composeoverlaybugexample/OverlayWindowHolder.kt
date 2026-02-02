package com.example.composeoverlaybugexample

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay


class OverlayWindowHolder {
    private var composeView: ComposeView? = null
    var stateOwner: CustomOwner
    var viewGroup: ViewGroup?
    val vmStore = ViewModelStore()

    constructor(context: Context) {
        viewGroup = FrameLayout(context)
        composeView = ComposeView(context)
        composeView?.setContent {
            var counter by remember { mutableIntStateOf(0) }
            LaunchedEffect(null) {
                while (true) {
                    counter += 1
                    delay(100)
                }
            }
            Box(Modifier.background(Color(0f, 0f, 0f, 0.75f)).height(48.dp).padding(horizontal = 16.dp), Alignment.Center) {
                Text(counter.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        stateOwner = CustomOwner()
        stateOwner.performRestore(null)
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        viewGroup?.setViewTreeLifecycleOwner(stateOwner)
        viewGroup?.setViewTreeSavedStateRegistryOwner(stateOwner)
        viewGroup?.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = vmStore })
        viewGroup?.addView(composeView)

        (context.getSystemService(WINDOW_SERVICE) as WindowManager).addView(
            viewGroup,
            WindowManager.LayoutParams().apply {
                width = WRAP_CONTENT
                height = WRAP_CONTENT
                x = 100
                y = 100
                gravity = Gravity.LEFT or Gravity.TOP
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.TRANSLUCENT
            }
        )
    }

    fun hideOverlay(context: Context) {
        (context.getSystemService(WINDOW_SERVICE) as WindowManager).removeView(viewGroup)
        viewGroup = null
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        stateOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

}


open class CustomOwner() : SavedStateRegistryOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    val savedStateRegistryController = SavedStateRegistryController.create(this)

    val isInitialized: Boolean
        get() = true

    override val lifecycle
        get() = lifecycleRegistry

    fun setCurrentState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun performSave(outBundle: Bundle) {
        savedStateRegistryController.performSave(outBundle)
    }
}















