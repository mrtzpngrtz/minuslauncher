package com.minimal.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minimal.launcher.ui.theme.MinimalLauncherTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinimalLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }
}

@Composable
fun HomeScreen(viewModel: AppListViewModel) {
    val context = LocalContext.current
    val apps by viewModel.selectedApps.collectAsState(initial = emptyList())
    val activeNotifications by AppNotificationListener.activeNotifications.collectAsState()
    
    // Check notification permission
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val enabledListeners = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        val packageName = context.packageName
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            // Prompt user to enable notification access
            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Gear Icon (Top Right)
        IconButton(
            onClick = { 
                context.startActivity(Intent(context, SettingsActivity::class.java))
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ClockAndDate()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (apps.isEmpty()) {
                Text(
                    text = "No apps selected.\nTap the gear icon to add apps.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(apps) { app ->
                        AppItem(
                            app = app, 
                            context = context,
                            hasNotification = activeNotifications.contains(app.packageName)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockAndDate() {
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = Calendar.getInstance().time
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = timeFormat.format(currentTime),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = dateFormat.format(currentTime),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun AppItem(app: AppInfo, context: Context, hasNotification: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                launchIntent?.let { context.startActivity(it) }
            }
    ) {
        Text(
            text = app.getDisplayName().lowercase(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start
        )
        
        if (hasNotification) {
            Text(
                text = " ⋮",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start
            )
        }
    }
}
