package com.example.composeoverlaybugexample

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.ContentPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.composeoverlaybugexample.ui.theme._ComposeOverlayBugExampleTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var overlayLoopStarted by mutableStateOf(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _ComposeOverlayBugExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
                        val context = LocalContext.current

                        Spacer(Modifier.height(40.dp))
                        Text("_Compose Overlay Bug Example", Modifier.padding(horizontal = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Title("1) Give permissions")
                        PermissionRow(OverlayPermission.isGranted().value, "Can draw overlays...") { OverlayPermission.openInSettings(context) }
                        PermissionRow(NotificationListenerPermission.isGranted().value, "Notification Listener...") { NotificationListenerPermission.openInSettings(context) }
                        PermissionRow(UnrestrictedBatteryUsagePermission.isGranted().value, "Unrestricted battery usage...") { UnrestrictedBatteryUsagePermission.openInSettings(context) }
                        Spacer(Modifier.height(20.dp))

                        Title("2) Start overlay loop")
                        var buttonEnabled by remember { mutableStateOf(true) }
                        val coroutine = rememberCoroutineScope()
                        Button(
                            onClick = {
                                overlayLoopStarted = !overlayLoopStarted
                                if (overlayLoopStarted) {
                                    GlobalScope.launch {
                                        while (true) {
                                            OverlayService.showOverlay(context)
                                            delay(1000)
                                            OverlayService.hideOverlay(context)
                                            delay(1000)
                                            if (!overlayLoopStarted) { return@launch }
                                        }
                                    }
                                } else {
                                    buttonEnabled = false
                                    coroutine.launch {
                                        delay(2000)
                                        buttonEnabled = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            enabled = buttonEnabled,
                        ) {
                            Text(if(!overlayLoopStarted && buttonEnabled) "START" else "STOP")
                        }
                        Spacer(Modifier.height(20.dp))

                        Title("3) Open other app while overlay is showing/hiding")
                        Spacer(Modifier.height(20.dp))

                        Title("4) Result (on Samsung Android 16):")
                        Text("New overlay ComposeView sometimes never recomposes (i.e., number doesn't increase)", Modifier.padding(horizontal = 16.dp))
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}


object OverlayPermission {
    @Composable fun isGranted(): State<Boolean> {
        val context = LocalContext.current
        return reloadOnResume { Settings.canDrawOverlays(context) }
    }
    fun openInSettings(context: Context) {
        val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        context.startActivity(myIntent)
    }
}


object UnrestrictedBatteryUsagePermission {
    @Composable fun isGranted(): State<Boolean> {
        val context = LocalContext.current
        return reloadOnResume {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return@reloadOnResume powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
    fun openInSettings(context: Context) {
        try {
            val requestPermissionIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            requestPermissionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(requestPermissionIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}


object NotificationListenerPermission {
    @Composable  fun isGranted(): State<Boolean> {
        val context = LocalContext.current
        return reloadOnResume {
            val packageNamesSet = NotificationManagerCompat.getEnabledListenerPackages(context)
            val isContained = packageNamesSet.contains(context.applicationContext.packageName)
            return@reloadOnResume isContained
        }
    }
    fun openInSettings(context: Context) {
        try {
            val settingsIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}


@Composable private fun reloadOnResume(valueFactory: ()->Boolean): State<Boolean> {
    val res = remember { mutableStateOf(valueFactory()) }
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                res.value = valueFactory()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return res
}


@Composable
fun PermissionRow(isGrated: Boolean, text: String, onClick: ()->Unit) {
    Row(Modifier.padding(horizontal = 16.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        Text(if (isGrated) "✅" else "❌️")
        Button(onClick = onClick) { Text(text) }
    }
}


@Composable
fun Title(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontWeight = FontWeight.SemiBold)
}














