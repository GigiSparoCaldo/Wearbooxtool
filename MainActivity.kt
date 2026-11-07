package com.oneplus.weartoolbox

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavScreen {
    HOME,
    CONNECTION,
    CONSOLE,
    APPS_TYPE,
    APPS_LIST,
    APP_DETAIL,
    APP_ACTIVITIES,
    APP_LOGCAT,
    FILES,
    BATTERY_INFO,
    BATTERY_USAGE,
    BATTERY_UID_DETAIL,
    DPI_MANAGER,
    INFO,
    POWER
}

enum class AppCategory { USER, SYSTEM, DISABLED }

data class AppItem(
    val label: String,
    val packageName: String,
    val isSystem: Boolean,
    val isEnabled: Boolean
)

// Palette Colori identica agli screenshot forniti
private val ColorConnectedGreen = Color(0xFF00C853)
private val ColorConsoleGray = Color(0xFF4F4F4F)
private val ColorAppsBlue = Color(0xFF0066FF)
private val ColorFilesTeal = Color(0xFF0097A7)
private val ColorBatteryGreen = Color(0xFF43A047)
private val ColorDpiBrown = Color(0xFF6D4C41)
private val ColorInfoPurple = Color(0xFF8E24AA)
private val ColorPowerAmber = Color(0xFFB26A00)
private val ColorSystemRed = Color(0xFFD32F2F)
private val ColorCardDark = Color(0xFF262626)
private val ColorUsageOrange = Color(0xFFFF9800)
private val ColorFolderYellow = Color(0xFFFFCA28)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colors = Colors(
                    primary = ColorAppsBlue,
                    background = Color.Black,
                    surface = ColorCardDark,
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                WearToolBoxApp()
            }
        }
    }
}

@Composable
fun WearToolBoxApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(NavScreen.HOME) }
    var statusText by remember { mutableStateOf("Connecting...") }
    var selectedCategory by remember { mutableStateOf(AppCategory.USER) }
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedBatteryUid by remember { mutableStateOf<BatteryUidUsage?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            HybridShellEngine.autoConnectBestAvailable(context)
            statusText = HybridShellEngine.getStatusLabel(context)
        }
    }

    when (screen) {
        NavScreen.HOME -> HomeScreen(
            statusText = statusText,
            onOpenConnection = { screen = NavScreen.CONNECTION },
            onNavigate = { screen = it }
        )

        NavScreen.CONNECTION -> ConnectionManagerScreen(
            onBack = {
                statusText = HybridShellEngine.getStatusLabel(context)
                screen = NavScreen.HOME
            }
        )

        NavScreen.CONSOLE -> ConsoleAndBuilderScreen(
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.APPS_TYPE -> AppTypeSelectorScreen(
            onSelectCategory = { cat ->
                selectedCategory = cat
                screen = NavScreen.APPS_LIST
            },
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.APPS_LIST -> AppListScreen(
            category = selectedCategory,
            onSelectApp = { app ->
                selectedApp = app
                screen = NavScreen.APP_DETAIL
            },
            onBack = { screen = NavScreen.APPS_TYPE }
        )

        NavScreen.APP_DETAIL -> {
            val app = selectedApp
            if (app != null) {
                AppDetailDebloaterScreen(
                    app = app,
                    onOpenActivities = { screen = NavScreen.APP_ACTIVITIES },
                    onOpenLogcat = { screen = NavScreen.APP_LOGCAT },
                    onBack = { screen = NavScreen.APPS_LIST }
                )
            }
        }

        NavScreen.APP_ACTIVITIES -> {
            val app = selectedApp
            if (app != null) {
                AppActivitiesScreen(
                    app = app,
                    onBack = { screen = NavScreen.APP_DETAIL }
                )
            }
        }

        NavScreen.APP_LOGCAT -> {
            val app = selectedApp
            if (app != null) {
                AppLogcatScreen(
                    app = app,
                    onBack = { screen = NavScreen.APP_DETAIL }
                )
            }
        }

        NavScreen.FILES -> FileManagerScreen(
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.BATTERY_INFO -> BatteryInfoScreen(
            onOpenUsage = { screen = NavScreen.BATTERY_USAGE },
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.BATTERY_USAGE -> BatteryUsageListScreen(
            onSelectUid = { item ->
                selectedBatteryUid = item
                screen = NavScreen.BATTERY_UID_DETAIL
            },
            onBack = { screen = NavScreen.BATTERY_INFO }
        )

        NavScreen.BATTERY_UID_DETAIL -> {
            val item = selectedBatteryUid
            if (item != null) {
                BatteryUidDetailScreen(
                    item = item,
                    onBack = { screen = NavScreen.BATTERY_USAGE }
                )
            }
        }

        NavScreen.DPI_MANAGER -> DpiManagerScreen(
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.INFO -> DeviceInfoScreen(
            onBack = { screen = NavScreen.HOME }
        )

        NavScreen.POWER -> PowerMenuScreen(
            onBack = { screen = NavScreen.HOME }
        )
    }
}

/**
 * 0. HOME MENU (Come in Foto 1 e Foto 2)
 */
@Composable
fun HomeScreen(
    statusText: String,
    onOpenConnection: () -> Unit,
    onNavigate: (NavScreen) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent {
                scope.launch { listState.scrollBy(it.verticalScrollPixels) }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Connected!",
                color = ColorConnectedGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 2.dp)
                    .clickable { onOpenConnection() }
            )
        }

        item {
            PillMenuButton("Console", Icons.Default.Terminal, ColorConsoleGray) {
                onNavigate(NavScreen.CONSOLE)
            }
        }
        item {
            PillMenuButton("Apps", Icons.Default.Apps, ColorAppsBlue) {
                onNavigate(NavScreen.APPS_TYPE)
            }
        }
        item {
            PillMenuButton("Files", Icons.Default.Folder, ColorFilesTeal) {
                onNavigate(NavScreen.FILES)
            }
        }
        item {
            PillMenuButton("Battery", Icons.Default.BatteryFull, ColorBatteryGreen) {
                onNavigate(NavScreen.BATTERY_INFO)
            }
        }
        item {
            PillMenuButton("DPI Manager", Icons.Default.AspectRatio, ColorDpiBrown) {
                onNavigate(NavScreen.DPI_MANAGER)
            }
        }
        item {
            PillMenuButton("Info", Icons.Default.Info, ColorInfoPurple) {
                onNavigate(NavScreen.INFO)
            }
        }
        item {
            PillMenuButton("Power", Icons.Default.PowerSettingsNew, ColorPowerAmber) {
                onNavigate(NavScreen.POWER)
            }
        }
        item {
            Text(
                text = statusText,
                color = Color.Gray,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .clickable { onOpenConnection() }
            )
        }
    }
}

@Composable
fun PillMenuButton(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 1. CONNECTION MANAGER (NSD Auto-Discovery + Built-in Pairing + Connection History)
 */
@Composable
fun ConnectionManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    var discoveredService by remember { mutableStateOf("Scanning NSD...") }
    var portInput by remember { mutableStateOf("5555") }
    var pairingCode by remember { mutableStateOf("") }
    var logMsg by remember { mutableStateOf(HybridShellEngine.getStatusLabel(context)) }
    var history by remember { mutableStateOf(HybridShellEngine.getConnectionHistory(context)) }

    DisposableEffect(Unit) {
        val listener = HybridShellEngine.discoverAdbServices(context) { host, port ->
            discoveredService = "$host:$port"
            portInput = port.toString()
        }
        onDispose {
            try {
                val nsd = context.getSystemService(Context.NSD_SERVICE) as android.net.nsd.NsdManager
                if (listener != null) nsd.stopServiceDiscovery(listener)
            } catch (_: Exception) {}
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            ScreenHeader("Connection Mgr", onBack)
        }

        item {
            Text(
                text = logMsg,
                color = ColorConnectedGreen,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        item {
            InfoRowCard("NSD Auto", discoveredService)
        }

        // Selettore Porta e Codice Pairing direttamente sull'orologio
        item {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Port: $portInput", color = Color.White, fontSize = 12.sp)
                CompactChip(
                    onClick = {
                        val ports = listOf("5555", "37845", "41235", "44555")
                        portInput = ports[(ports.indexOf(portInput) + 1) % ports.size]
                    },
                    label = { Text("Change", fontSize = 10.sp) }
                )
            }
        }

        item {
            PillMenuButton("Connect 127.0.0.1", Icons.Default.Link, ColorAppsBlue) {
                scope.launch {
                    logMsg = "Connecting to 127.0.0.1:$portInput..."
                    val res = withContext(Dispatchers.IO) {
                        HybridShellEngine.connectAdb(context, "127.0.0.1", portInput.toIntOrNull() ?: 5555)
                    }
                    logMsg = if (res.isSuccess) "Connected! ${res.getOrNull()}" else "Failed: ${res.exceptionOrNull()?.message}"
                    history = HybridShellEngine.getConnectionHistory(context)
                }
            }
        }

        // Built-in Pairing Pad (Inserimento codice a 6 cifre direttamente sull'orologio)
        item {
            Text("Built-in Wireless Pairing", color = ColorAppsBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                text = "Code: ${pairingCode.ifEmpty { "------" }}",
                color = ColorFolderYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { n ->
                    SmallDigitButton(n.toString()) { if (pairingCode.length < 6) pairingCode += n }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (6..9).forEach { n ->
                    SmallDigitButton(n.toString()) { if (pairingCode.length < 6) pairingCode += n }
                }
                SmallDigitButton("0") { if (pairingCode.length < 6) pairingCode += "0" }
                SmallDigitButton("C") { pairingCode = "" }
            }
        }
        item {
            CompactChip(
                onClick = {
                    scope.launch {
                        val out = withContext(Dispatchers.IO) {
                            HybridShellEngine.exec("adb pair 127.0.0.1:$portInput $pairingCode")
                        }
                        logMsg = out
                    }
                },
                label = { Text("Pair on Watch", fontSize = 11.sp) }
            )
        }

        // Connection History
        item {
            Text("Saved History", color = Color.Gray, fontSize = 11.sp)
        }
        items(history) { endpoint ->
            InfoRowCard("Endpoint", endpoint) {
                val parts = endpoint.split(":")
                scope.launch {
                    withContext(Dispatchers.IO) {
                        HybridShellEngine.connectAdb(
                            context,
                            parts.getOrNull(0) ?: "127.0.0.1",
                            parts.getOrNull(1)?.toIntOrNull() ?: 5555
                        )
                    }
                    logMsg = HybridShellEngine.getStatusLabel(context)
                }
            }
        }
    }
}

@Composable
fun SmallDigitButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(ColorCardDark)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 2. APP MANAGER - Selettore Categoria (Identico a Foto 3: User Blu, System Rosso, Disabled Grigio)
 */
@Composable
fun AppTypeSelectorScreen(
    onSelectCategory: (AppCategory) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "App Type",
                color = Color.LightGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .clickable { onBack() }
            )
        }

        item {
            PillMenuButton("User", Icons.Default.Person, ColorAppsBlue) {
                onSelectCategory(AppCategory.USER)
            }
        }
        item {
            PillMenuButton("System", Icons.Default.Android, ColorSystemRed) {
                onSelectCategory(AppCategory.SYSTEM)
            }
        }
        item {
            PillMenuButton("Disabled", Icons.Default.Block, ColorConsoleGray) {
                onSelectCategory(AppCategory.DISABLED)
            }
        }
    }
}

@Composable
fun AppListScreen(
    category: AppCategory,
    onSelectApp: (AppItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(category) {
        loading = true
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val admin = AdminReceiver.getComponentName(context)
            val isOwner = dpm.isDeviceOwnerApp(context.packageName)

            pm.getInstalledApplications(flags).map { ai ->
                val isSys = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hidden = if (isOwner) {
                    try { dpm.isApplicationHidden(admin, ai.packageName) } catch (_: Exception) { false }
                } else false
                val enabled = ai.enabled && !hidden
                val label = try { pm.getApplicationLabel(ai).toString() } catch (_: Exception) { ai.packageName }
                AppItem(label, ai.packageName, isSys, enabled)
            }.filter {
                when (category) {
                    AppCategory.USER -> !it.isSystem && it.isEnabled
                    AppCategory.SYSTEM -> it.isSystem && it.isEnabled
                    AppCategory.DISABLED -> !it.isEnabled
                }
            }.sortedBy { it.label.lowercase() }
        }
        loading = false
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { ScreenHeader(category.name, onBack) }

        if (loading) {
            item { CircularProgressIndicator(indicatorColor = ColorAppsBlue) }
        } else {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ColorCardDark)
                        .clickable { onSelectApp(app) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(app.packageName, color = Color.Gray, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AppDetailDebloaterScreen(
    app: AppItem,
    onOpenActivities: () -> Unit,
    onOpenLogcat: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    var enabledState by remember { mutableStateOf(app.isEnabled) }
    var actionOutput by remember { mutableStateOf("") }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { ScreenHeader(app.label, onBack) }
        item { Text(app.packageName, color = Color.Gray, fontSize = 9.sp, textAlign = TextAlign.Center) }

        if (actionOutput.isNotEmpty()) {
            item {
                Text(actionOutput, color = ColorConnectedGreen, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }

        // Disable / Enable
        item {
            PillMenuButton(
                title = if (enabledState) "Disable" else "Enable",
                icon = Icons.Default.Block,
                bgColor = if (enabledState) ColorPowerAmber else ColorBatteryGreen
            ) {
                scope.launch {
                    val newTarget = !enabledState
                    actionOutput = withContext(Dispatchers.IO) {
                        HybridShellEngine.setPackageEnabled(context, app.packageName, newTarget)
                    }
                    enabledState = newTarget
                }
            }
        }

        // Force Stop
        item {
            PillMenuButton("Force Stop", Icons.Default.StopCircle, ColorSystemRed) {
                scope.launch {
                    actionOutput = withContext(Dispatchers.IO) {
                        HybridShellEngine.forceStopApp(context, app.packageName)
                    }
                }
            }
        }

        // Clear Data
        item {
            PillMenuButton("Clear Data", Icons.Default.DeleteSweep, ColorDpiBrown) {
                scope.launch {
                    actionOutput = withContext(Dispatchers.IO) {
                        HybridShellEngine.exec("pm clear ${app.packageName}")
                    }
                }
            }
        }

        // Uninstall (pm uninstall -k --user 0)
        item {
            PillMenuButton("Uninstall", Icons.Default.DeleteForever, ColorSystemRed) {
                scope.launch {
                    actionOutput = withContext(Dispatchers.IO) {
                        HybridShellEngine.exec("pm uninstall --user 0 ${app.packageName}")
                    }
                }
            }
        }

        // Launch Specific Activities
        item {
            PillMenuButton("Activities", Icons.Default.Launch, ColorAppsBlue) {
                onOpenActivities()
            }
        }

        // View Logcat for this app
        item {
            PillMenuButton("App Logcat", Icons.Default.BugReport, ColorConsoleGray) {
                onOpenLogcat()
            }
        }
    }
}

@Composable
fun AppActivitiesScreen(app: AppItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val activities = remember(app.packageName) {
        try {
            val pi = context.packageManager.getPackageInfo(
                app.packageName,
                PackageManager.GET_ACTIVITIES
            )
            pi.activities?.map { it.name } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { ScreenHeader("Activities", onBack) }

        if (activities.isEmpty()) {
            item { Text("No public activities found", color = Color.Gray, fontSize = 11.sp) }
        } else {
            items(activities) { actName ->
                val shortName = actName.substringAfterLast(".")
                InfoRowCard(shortName, "Launch") {
                    scope.launch {
                        try {
                            val intent = Intent().apply {
                                component = ComponentName(app.packageName, actName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            withContext(Dispatchers.IO) {
                                HybridShellEngine.exec("am start -n ${app.packageName}/$actName")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLogcatScreen(app: AppItem, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    var logs by remember { mutableStateOf("Loading logcat...") }

    LaunchedEffect(app.packageName) {
        logs = withContext(Dispatchers.IO) {
            val pid = HybridShellEngine.exec("pidof -s ${app.packageName}").trim()
            val cmd = if (pid.isNotEmpty() && pid.all { it.isDigit() }) {
                "logcat -d --pid=$pid -t 60"
            } else {
                "logcat -d -t 80 | grep -i ${app.packageName}"
            }
            HybridShellEngine.exec(cmd).ifBlank { "No recent logcat lines for ${app.packageName}" }
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { ScreenHeader("Logcat", onBack) }
        item {
            Text(
                text = logs,
                color = ColorConnectedGreen,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/**
 * 3. DPI MANAGER (Come in Foto 8 con timer automatico di 5 secondi e conferma verde/grigia)
 */
@Composable
fun DpiManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val defaultDpi = remember { context.resources.displayMetrics.densityDpi }
    var currentDpi by remember { mutableIntStateOf(defaultDpi) }
    var targetDpi by remember { mutableIntStateOf(defaultDpi) }
    var previousSafeDpi by remember { mutableIntStateOf(defaultDpi) }
    var countdownSec by remember { mutableIntStateOf(0) }

    // Safety Feature: Auto-revert in 5 seconds if not confirmed!
    LaunchedEffect(countdownSec) {
        if (countdownSec > 0) {
            delay(1000L)
            countdownSec -= 1
            if (countdownSec == 0) {
                withContext(Dispatchers.IO) {
                    HybridShellEngine.exec("wm density $previousSafeDpi")
                }
                currentDpi = previousSafeDpi
                targetDpi = previousSafeDpi
                Toast.makeText(context, "Auto-reverted to $previousSafeDpi DPI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { ScreenHeader("DPI Manager", onBack) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141414))
                        .padding(10.dp)
                ) {
                    Text("Danger!", color = ColorSystemRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Changing screen density (DPI) can make the interface too small or unusable.\nMinimum limit: 120 DPI.\nIf something goes wrong, do NOT press anything — settings will revert automatically in 5 sec.",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }

            item {
                Text("Current: $currentDpi", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Selettore rapido valore DPI senza tastiera
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallDigitButton("-") { targetDpi = (targetDpi - 20).coerceAtLeast(120) }
                    Text("$targetDpi DPI", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    SmallDigitButton("+") { targetDpi = (targetDpi + 20).coerceAtMost(480) }
                }
            }

            item {
                PillMenuButton("Apply $targetDpi DPI", Icons.Default.Send, ColorDpiBrown) {
                    previousSafeDpi = currentDpi
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            HybridShellEngine.exec("wm density $targetDpi")
                        }
                        currentDpi = targetDpi
                        countdownSec = 5
                    }
                }
            }

            item {
                CompactChip(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                HybridShellEngine.exec("wm density reset")
                            }
                            currentDpi = defaultDpi
                            targetDpi = defaultDpi
                        }
                    },
                    label = { Text("Reset to Default", fontSize = 11.sp) }
                )
            }
        }

        // Overlay di Sicurezza 5-Second Auto-Revert (Identico a Foto 8)
        if (countdownSec > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Keep new DPI?\nAuto-revert in: $countdownSec",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Pulsante Revert immediato (Grigio con icona History/Restore)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A3A3A))
                                .clickable {
                                    countdownSec = 0
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            HybridShellEngine.exec("wm density $previousSafeDpi")
                                        }
                                        currentDpi = previousSafeDpi
                                        targetDpi = previousSafeDpi
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "Revert", tint = Color.White)
                        }

                        // Pulsante Conferma (Verde con Pollice Su come in Foto 8)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ColorBatteryGreen)
                                .clickable {
                                    countdownSec = 0
                                    previousSafeDpi = currentDpi
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Keep", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. CONSOLE & GUI COMMAND BUILDER (Con conferma "Execute")
 */
@Composable
fun ConsoleAndBuilderScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val categories = listOf("pm (Packages)", "wm (Display)", "settings", "dumpsys", "am (Activity)")
    var selectedCatIdx by remember { mutableIntStateOf(0) }

    val presetCommands = remember(selectedCatIdx) {
        when (selectedCatIdx) {
            0 -> listOf("pm list packages -s", "pm list packages -3", "pm list packages -d", "pm trim-caches 999G")
            1 -> listOf("wm size", "wm density", "wm density reset", "wm overscan reset")
            2 -> listOf(
                "settings put global window_animation_scale 0.5",
                "settings put global transition_animation_scale 0.5",
                "settings put global animator_duration_scale 0.5",
                "settings put global adb_wifi_enabled 1"
            )
            3 -> listOf("dumpsys battery", "dumpsys meminfo", "dumpsys deviceidle", "dumpsys alarm")
            else -> listOf("am kill-all", "ps -A", "top -n 1 -m 10", "getprop ro.product.model")
        }
    }

    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var terminalOutput by remember { mutableStateOf("Ready. Select a command from GUI Builder.") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { ScreenHeader("Console & Builder", onBack) }

            item {
                CompactChip(
                    onClick = { selectedCatIdx = (selectedCatIdx + 1) % categories.size },
                    label = { Text("Category: ${categories[selectedCatIdx]}", fontSize = 11.sp) },
                    colors = ChipDefaults.primaryChipColors(backgroundColor = ColorAppsBlue)
                )
            }

            items(presetCommands) { cmd ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ColorCardDark)
                        .clickable { pendingCommand = cmd }
                        .padding(10.dp)
                ) {
                    Text(cmd, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101010))
                        .padding(10.dp)
                ) {
                    Text("Terminal Output:", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = terminalOutput,
                        color = ColorConnectedGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Conferma "Execute" per evitare esecuzioni accidentali
        if (pendingCommand != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Execute Command?", color = ColorFolderYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pendingCommand!!,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompactChip(
                            onClick = { pendingCommand = null },
                            label = { Text("Cancel", fontSize = 11.sp) }
                        )
                        CompactChip(
                            onClick = {
                                val cmdToRun = pendingCommand!!
                                pendingCommand = null
                                terminalOutput = "Running: $cmdToRun..."
                                scope.launch {
                                    terminalOutput = withContext(Dispatchers.IO) {
                                        HybridShellEngine.exec(cmdToRun)
                                    }
                                }
                            },
                            label = { Text("Execute", fontSize = 11.sp) },
                            colors = ChipDefaults.primaryChipColors(backgroundColor = ColorBatteryGreen)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. BATTERY & POWER (Identico a Foto 5, Foto 6, Foto 7 e Foto 10)
 */
@Composable
fun BatteryInfoScreen(
    onOpenUsage: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val batteryIntent = remember(refreshTrigger) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 83) ?: 83
    val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val statusStr = if (statusInt == BatteryManager.BATTERY_STATUS_CHARGING) "Charging" else "Discharging"
    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val sourceStr = if (plugged != 0) "AC / Wireless" else "Battery"
    val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4203) ?: 4203
    val voltageStr = String.format("%.3f V", voltageMv / 1000.0)
    val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 399) ?: 399
    val tempStr = String.format("%.1f°C", tempTenths / 10.0)
    val techStr = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Grande Percentuale Verde in cima (Foto 5)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = ColorConnectedGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "$level%",
                    color = ColorConnectedGreen,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { BatteryStatCleanRow("Status", statusStr) }
        item { BatteryStatCleanRow("Source", sourceStr) }
        item { BatteryStatCleanRow("Voltage", voltageStr) }
        item { BatteryStatCleanRow("Temperature", tempStr) }
        item { BatteryStatCleanRow("Technology", techStr) }

        // I due pulsanti circolari in basso (Refresh grigio + Grafico torta blu) come in Foto 5
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .clickable { refreshTrigger++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ColorAppsBlue)
                        .clickable { onOpenUsage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = "Usage", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun BatteryStatCleanRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Schermata Usage (Identica a Foto 6: 3 icone filtro circolari + card con barra arancione)
 */
@Composable
fun BatteryUsageListScreen(
    onSelectUid: (BatteryUidUsage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var allStats by remember { mutableStateOf<List<BatteryUidUsage>>(emptyList()) }
    var filterMode by remember { mutableIntStateOf(1) } // 0 = User, 1 = System (Android icon), 2 = All/Sensors

    LaunchedEffect(Unit) {
        allStats = withContext(Dispatchers.IO) {
            HybridShellEngine.parseBatteryStats(context)
        }
    }

    val filtered = remember(allStats, filterMode) {
        when (filterMode) {
            0 -> allStats.filter { !it.isSystem }
            1 -> allStats.filter { it.isSystem }
            else -> allStats
        }
    }
    val maxMah = remember(filtered) { (filtered.maxOfOrNull { it.mAh } ?: 1.0).coerceAtLeast(1.0) }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Usage",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp).clickable { onBack() }
            )
        }

        // 3 Pulsanti filtro rotondi (Foto 6)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UsageFilterCircle(Icons.Default.Person, selected = filterMode == 0) { filterMode = 0 }
                UsageFilterCircle(Icons.Default.Android, selected = filterMode == 1) { filterMode = 1 }
                UsageFilterCircle(Icons.Default.Sensors, selected = filterMode == 2) { filterMode = 2 }
            }
        }

        items(filtered) { item ->
            val ratio = (item.mAh / maxMah).toFloat().coerceIn(0.08f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ColorCardDark)
                    .clickable { onSelectUid(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(String.format("%.1f", item.mAh), color = Color.LightGray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF424242))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ColorUsageOrange)
                    )
                }
            }
        }
    }
}

@Composable
fun UsageFilterCircle(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF3E2700) else Color(0xFF383838))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) ColorUsageOrange else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Dettaglio Consumo Singolo UID + Wakelocks (Identico a Foto 7)
 */
@Composable
fun BatteryUidDetailScreen(item: BatteryUidUsage, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = item.name,
                color = Color.LightGray,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp).clickable { onBack() }
            )
        }
        item {
            Text("UID: ${item.uid}", color = Color.Gray, fontSize = 12.sp)
        }
        item {
            Text(
                text = String.format("%.2f mAh", item.mAh),
                color = ColorUsageOrange,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            Text("8.6% of total", color = Color.Gray, fontSize = 13.sp)
        }
        item {
            Text(
                text = "Wakelocks:",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(item.wakelocks) { wl ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF181818))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(wl, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * 6. FILE MANAGER (Identico a Foto 4: sdcard in cima, cartelle gialle e cancellazione file)
 */
@Composable
fun FileManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    fun reloadFiles() {
        files = currentDir.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    LaunchedEffect(currentDir) {
        reloadFiles()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = if (currentDir == Environment.getExternalStorageDirectory()) "sdcard" else currentDir.name,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 2.dp)
                        .clickable {
                            val parent = currentDir.parentFile
                            if (currentDir != Environment.getExternalStorageDirectory() && parent != null) {
                                currentDir = parent
                            } else {
                                onBack()
                            }
                        }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                item {
                    CompactChip(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        label = { Text("Grant Storage Access", fontSize = 10.sp) }
                    )
                }
            }

            items(files, key = { it.absolutePath }) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(ColorCardDark)
                        .clickable {
                            if (file.isDirectory) {
                                currentDir = file
                            } else {
                                fileToDelete = file
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (file.isDirectory) ColorFolderYellow else Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file.name,
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { fileToDelete = file }
                    )
                }
            }
        }

        // Dialog di conferma eliminazione file/cartella
        if (fileToDelete != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Delete ${fileToDelete!!.name}?", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactChip(
                            onClick = { fileToDelete = null },
                            label = { Text("Cancel", fontSize = 11.sp) }
                        )
                        CompactChip(
                            onClick = {
                                val target = fileToDelete!!
                                fileToDelete = null
                                target.deleteRecursively()
                                reloadFiles()
                            },
                            label = { Text("Delete", fontSize = 11.sp) },
                            colors = ChipDefaults.primaryChipColors(backgroundColor = ColorSystemRed)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7. INFO SCREEN (Identico a Foto 9: Device blu, card Model / Brand / Name, System blu)
 */
@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "Info",
                color = Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp).clickable { onBack() }
            )
        }
        item {
            Text("Device", color = ColorAppsBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        item { InfoRowCard("Model", Build.MODEL) }
        item { InfoRowCard("Brand", Build.BRAND) }
        item { InfoRowCard("Name", Build.PRODUCT) }

        item {
            Text(
                text = "System",
                color = ColorAppsBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item { InfoRowCard("Android", Build.VERSION.RELEASE) }
        item { InfoRowCard("SDK", Build.VERSION.SDK_INT.toString()) }
        item { InfoRowCard("Security", Build.VERSION.SECURITY_PATCH) }
    }
}

@Composable
fun InfoRowCard(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1C))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/**
 * 8. POWER MENU (Identico a Foto 10: Reboot Verde, Recovery Grigio, Bootloader Grigio)
 */
@Composable
fun PowerMenuScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Power Menu",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).clickable { onBack() }
            )
        }

        item {
            PowerActionPill("Reboot", ColorBatteryGreen) {
                scope.launch {
                    withContext(Dispatchers.IO) { HybridShellEngine.rebootWatch(context, "") }
                }
            }
        }
        item {
            PowerActionPill("Recovery", ColorCardDark) {
                scope.launch {
                    withContext(Dispatchers.IO) { HybridShellEngine.rebootWatch(context, "recovery") }
                }
            }
        }
        item {
            PowerActionPill("Bootloader", ColorCardDark) {
                scope.launch {
                    withContext(Dispatchers.IO) { HybridShellEngine.rebootWatch(context, "bootloader") }
                }
            }
        }
    }
}

@Composable
fun PowerActionPill(title: String, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(top = 12.dp, bottom = 4.dp)
            .clickable { onBack() }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
