package com.example.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.FocusScheduleSlot
import com.example.data.preferences.ShieldPreferences
import com.example.ui.components.WeeklyActivityCard
import com.example.ui.focus.SmartFocusDashboardCard
import com.example.ui.security.BiometricAndPrivacyDashboardSection
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextSecondary
import kotlin.math.roundToInt

private enum class PendingAntiTamperAction {
    DISABLE_MASTER_SHIELD,
    DISABLE_STRICT_MODE
}

@Composable
fun DashboardScreen(
    isProtectionEnabled: Boolean,
    lockDurationMinutes: Int,
    strictModeEnabled: Boolean,
    streakDays: Int,
    incidentCount: Int,
    totalKeywordsCount: Int,
    incidents: List<BlockIncident> = emptyList(),
    appLimits: List<AppUsageLimit> = emptyList(),
    focusSlots: List<FocusScheduleSlot> = emptyList(),
    isSmartFocusEnabled: Boolean = true,
    isBiometricLockEnabled: Boolean = false,
    isBlackoutPrivacyShieldEnabled: Boolean = true,
    screenshotTempUnlockUntil: Long = 0L,
    preferences: ShieldPreferences? = null,
    usageRefreshTrigger: Long = 0L,
    onToggleProtection: (Boolean) -> Unit,
    onChangeDuration: (Int) -> Unit,
    onToggleStrictMode: (Boolean) -> Unit,
    onToggleSmartFocusMaster: (Boolean) -> Unit = {},
    onToggleFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onAddFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onStartFocusSessionNow: (title: String, durationMinutes: Int) -> Unit = { _, _ -> },
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    onToggleBlackoutShield: (Boolean) -> Unit = {},
    onAllowScreenshot60s: () -> Unit = {},
    onOpenTestSandbox: () -> Unit,
    onOpenPanicMode: () -> Unit = {},
    onResetAppFromScratch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var isAccessibilityEnabled by remember {
        mutableStateOf(ShieldPreferences.isAccessibilityServiceEnabled(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(ShieldPreferences.hasOverlayPermission(context))
    }
    var isBatteryOptExempt by remember {
        mutableStateOf(ShieldPreferences.isIgnoringBatteryOptimizations(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    var pendingAntiTamperAction by remember { mutableStateOf<PendingAntiTamperAction?>(null) }
    var showResetFromScratchDialog by remember { mutableStateOf(false) }
    var resetSuccessBannerVisible by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            com.example.notification.LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
        }
    }

    // Refresh permissions automatically whenever user returns from system Settings
    val checkPermissions = {
        isAccessibilityEnabled = ShieldPreferences.isAccessibilityServiceEnabled(context)
        hasOverlayPermission = ShieldPreferences.hasOverlayPermission(context)
        isBatteryOptExempt = ShieldPreferences.isIgnoringBatteryOptimizations(context)
        hasNotificationPermission = checkNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title & Master Shield Switch (Aurora Glass Hero)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isProtectionEnabled) EmeraldShield.copy(alpha = 0.45f) else CrimsonPrimary.copy(alpha = 0.55f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isProtectionEnabled) listOf(
                                EmeraldShield.copy(alpha = 0.18f),
                                CyanTelemetry.copy(alpha = 0.08f),
                                Color.Transparent
                            ) else listOf(
                                CrimsonPrimary.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isProtectionEnabled) EmeraldShield.copy(alpha = 0.2f)
                                    else CrimsonPrimary.copy(alpha = 0.2f)
                                )
                                .border(
                                    2.dp,
                                    if (isProtectionEnabled) EmeraldShield else CrimsonPrimary,
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isProtectionEnabled) Icons.Default.Shield else Icons.Default.Warning,
                                contentDescription = "Shield Status",
                                tint = if (isProtectionEnabled) EmeraldShield else CrimsonPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ShieldBlock",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isProtectionEnabled) EmeraldShield.copy(alpha = 0.2f)
                                    else CrimsonPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isProtectionEnabled) "AEGIS ON" else "PAUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProtectionEnabled) EmeraldShield else CrimsonPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isProtectionEnabled) "Real-time AI & Accessibility Guard Active" else "Protection Paused — Tap to Resume",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isProtectionEnabled,
                        onCheckedChange = { targetEnabled ->
                            if (!targetEnabled && strictModeEnabled) {
                                pendingAntiTamperAction = PendingAntiTamperAction.DISABLE_MASTER_SHIELD
                            } else {
                                onToggleProtection(targetEnabled)
                            }
                        },
                        modifier = Modifier.testTag("protection_master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldShield,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = ObsidianBorder
                        )
                    )
                }
            }
        }

        // Emergency Panic Mode Action Card (with Always-On Notification SOS Status)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPanicMode() }
                .testTag("dashboard_panic_mode_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.85f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CrimsonPrimary.copy(alpha = 0.22f),
                                IndigoAccent.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CrimsonPrimary.copy(alpha = 0.25f))
                                    .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Emergency,
                                    contentDescription = "Emergency Panic Mode",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency Panic Mode (SOS)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Instant self-lockdown & 4-7-8 urge relief",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFB3C6)
                                )
                            }
                        }

                        Button(
                            onClick = onOpenPanicMode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dashboard_panic_button")
                        ) {
                            Text(
                                text = "PANIC",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Always-On Notification Bar SOS Indicator
                    Surface(
                        onClick = {
                            com.example.notification.LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCard.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Panic SOS button pinned in Notification Bar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianTextSecondary
                                )
                            }
                            Text(
                                text = "ALWAYS ON",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldShield,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Bento Stats Row (Streak, Blocks, Keywords)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "CLEAN STREAK",
                value = "${streakDays}d",
                subtitle = "Self-mastery",
                accentColor = EmeraldShield,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "BLOCKS",
                value = "$incidentCount",
                subtitle = "Triggers stopped",
                accentColor = CrimsonPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "ACTIVE RULES",
                value = "$totalKeywordsCount",
                subtitle = "EN + FA shield",
                accentColor = CyanTelemetry,
                modifier = Modifier.weight(1f)
            )
        }

        // 7-Day Weekly Activity Status Bento Card
        val effectivePrefs = remember(context, preferences) {
            preferences ?: ShieldPreferences(context)
        }
        WeeklyActivityCard(
            incidents = incidents,
            appLimits = appLimits,
            preferences = effectivePrefs,
            usageRefreshTrigger = usageRefreshTrigger
        )

        // Smart Focus (Deep Work Auto-Lock Slots) Card
        SmartFocusDashboardCard(
            focusSlots = focusSlots,
            isSmartFocusEnabled = isSmartFocusEnabled,
            onToggleSmartFocusMaster = onToggleSmartFocusMaster,
            onToggleFocusSlot = onToggleFocusSlot,
            onAddFocusSlot = onAddFocusSlot,
            onStartFocusSessionNow = onStartFocusSessionNow
        )

        // Biometric Lodge & Anti-Agent Black Screen Shield (FLAG_SECURE) Controls
        BiometricAndPrivacyDashboardSection(
            preferences = effectivePrefs,
            isBiometricLockEnabled = isBiometricLockEnabled,
            isBlackoutPrivacyShieldEnabled = isBlackoutPrivacyShieldEnabled,
            screenshotTempUnlockUntil = screenshotTempUnlockUntil,
            onToggleBiometricLock = onToggleBiometricLock,
            onLockAppNow = onLockAppNow,
            onToggleBlackoutShield = onToggleBlackoutShield,
            onAllowScreenshot60s = onAllowScreenshot60s
        )

        // Permission Setup Warning Banner
        if (!isAccessibilityEnabled || !hasOverlayPermission || !isBatteryOptExempt || !hasNotificationPermission) {
            PermissionRequiredCard(
                isAccessibilityEnabled = isAccessibilityEnabled,
                hasOverlayPermission = hasOverlayPermission,
                isBatteryOptExempt = isBatteryOptExempt,
                hasNotificationPermission = hasNotificationPermission,
                onEnableAccessibility = {
                    openAccessibilitySettings(context)
                    checkPermissions()
                },
                onEnableOverlay = {
                    openOverlaySettings(context)
                    checkPermissions()
                },
                onDisableBatteryOpt = {
                    ShieldPreferences.requestDisableBatteryOptimization(context)
                    checkPermissions()
                },
                onEnableNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        } else {
            AllProtectionsActiveCard()
        }

        // Dedicated Stop Battery Optimization Card
        BatteryOptimizationCard(
            isExempt = isBatteryOptExempt,
            onRequestExemption = {
                ShieldPreferences.requestDisableBatteryOptimization(context)
                checkPermissions()
            }
        )

        // Custom Lock Duration Picker (Central User Feature)
        LockDurationCard(
            currentMinutes = lockDurationMinutes,
            onChangeDuration = onChangeDuration
        )

        // Strict Lock Mode Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonDark.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Strict Lock Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Cannot bypass or dismiss until timer reaches 0",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Switch(
                    checked = strictModeEnabled,
                    onCheckedChange = { targetStrict ->
                        if (!targetStrict) {
                            pendingAntiTamperAction = PendingAntiTamperAction.DISABLE_STRICT_MODE
                        } else {
                            onToggleStrictMode(true)
                        }
                    },
                    modifier = Modifier.testTag("strict_mode_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CrimsonPrimary
                    )
                )
            }
        }

        // Safe Simulator / Testing Arena Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTestSandbox() }
                .testTag("open_sandbox_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Test Blocker / Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Test English/Persian keywords & Section Blockers safely",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open test",
                    tint = Color(0xFF818CF8)
                )
            }
        }

        // Reset App & Test From Scratch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showResetFromScratchDialog = true }
                .testTag("reset_app_from_scratch_button"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanTelemetry.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanTelemetry.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset App From Scratch",
                                tint = CyanTelemetry,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Test App From Scratch (Reset All)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Clear all timers, 2h cooldowns, usage logs, streaks & custom rules to start fresh",
                                style = MaterialTheme.typography.bodySmall,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showResetFromScratchDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanTelemetry,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (resetSuccessBannerVisible) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldShield.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldShield),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_from_scratch_success_banner")
                    ) {
                        Text(
                            text = "Fresh Start Complete! All app limits, 2h cooldowns, Smart Focus slots, streaks, and logs have been reset.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Informational Privacy Note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ObsidianTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "100% On-Device Protection. Your screen text is never transmitted or saved.",
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianTextSecondary
            )
        }
    }

    if (showResetFromScratchDialog) {
        AlertDialog(
            onDismissRequest = { showResetFromScratchDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "Reset App & Start From Scratch?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will wipe all accumulated usage timers, unlock any active 2-hour cooldowns, clear incident history, reset your clean streak to Day 0, and restore default App Limits, Section Rules, and Smart Focus slots so you can test the entire app from a clean slate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAppFromScratch()
                        showResetFromScratchDialog = false
                        resetSuccessBannerVisible = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_reset_from_scratch_button")
                ) {
                    Text("Reset Everything Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetFromScratchDialog = false }) {
                    Text("Cancel", color = ObsidianTextSecondary)
                }
            }
        )
    }

    if (pendingAntiTamperAction != null) {
        StrictModeCommitmentDialog(
            actionTitle = when (pendingAntiTamperAction) {
                PendingAntiTamperAction.DISABLE_MASTER_SHIELD -> "Pause Master Shield Protection?"
                PendingAntiTamperAction.DISABLE_STRICT_MODE -> "Disable Strict Lock Mode?"
                null -> ""
            },
            onConfirm = {
                when (pendingAntiTamperAction) {
                    PendingAntiTamperAction.DISABLE_MASTER_SHIELD -> onToggleProtection(false)
                    PendingAntiTamperAction.DISABLE_STRICT_MODE -> onToggleStrictMode(false)
                    null -> {}
                }
                pendingAntiTamperAction = null
            },
            onDismiss = { pendingAntiTamperAction = null }
        )
    }
}

@Composable
private fun StrictModeCommitmentDialog(
    actionTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val requiredPhrase = "I choose self control"
    var typedPhrase by remember { mutableStateOf("") }
    val isMatched = typedPhrase.trim().equals(requiredPhrase, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Strict Mode is active to protect your clean streak during moments of urge. To confirm this change, type the phrase below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"$requiredPhrase\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = typedPhrase,
                    onValueChange = { typedPhrase = it },
                    placeholder = { Text("Type phrase to unlock...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("strict_mode_confirm_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isMatched,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                modifier = Modifier.testTag("strict_mode_confirm_button")
            ) {
                Text("Confirm Disable", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Protected", color = EmeraldShield, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    fontSize = 9.sp,
                    color = ObsidianTextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun LockDurationCard(
    currentMinutes: Int,
    onChangeDuration: (Int) -> Unit
) {
    val presets = listOf(1, 5, 15, 30, 60)
    var sliderValue by remember(currentMinutes) { mutableFloatStateOf(currentMinutes.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lock Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonPrimary.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary)
                ) {
                    Text(
                        text = "$currentMinutes min",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "When explicit content is detected, the phone will stay locked for this duration:",
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            // Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { min ->
                    val isSelected = min == currentMinutes
                    Surface(
                        onClick = {
                            sliderValue = min.toFloat()
                            onChangeDuration(min)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("preset_${min}_min"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CrimsonPrimary else ObsidianCard,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (min == 1) "1m (Test)" else "${min}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else ObsidianTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Slider
            Text(
                text = "Custom Duration Slider (1 to 120 minutes):",
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianTextSecondary
            )

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onChangeDuration(it.roundToInt())
                },
                valueRange = 1f..120f,
                steps = 119,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duration_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = CrimsonPrimary,
                    activeTrackColor = CrimsonPrimary,
                    inactiveTrackColor = ObsidianBorder
                )
            )
        }
    }
}

@Composable
fun PermissionRequiredCard(
    isAccessibilityEnabled: Boolean,
    hasOverlayPermission: Boolean,
    isBatteryOptExempt: Boolean,
    hasNotificationPermission: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onEnableNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF231718)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Permission Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "To monitor on-screen text across browsers, lock over apps, and show live countdown alerts, ShieldBlock needs:",
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            // Accessibility permission item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. Accessibility Service",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isAccessibilityEnabled) "Active & scanning" else "Needed to read on-screen explicit words",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAccessibilityEnabled) EmeraldShield else ObsidianTextSecondary
                    )
                }

                if (isAccessibilityEnabled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onEnableAccessibility,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("enable_accessibility_button")
                    ) {
                        Text(
                            text = "Enable",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overlay permission item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. Display Over Other Apps",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (hasOverlayPermission) "Active & granted" else "Required to display lockdown screen over browsers",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasOverlayPermission) EmeraldShield else ObsidianTextSecondary
                    )
                }

                if (hasOverlayPermission) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onEnableOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("enable_overlay_button")
                    ) {
                        Text(
                            text = "Grant",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Battery Optimization Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. Stop Battery Optimization",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isBatteryOptExempt) "Unrestricted background active" else "Prevents Android from killing the scanner",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBatteryOptExempt) EmeraldShield else ObsidianTextSecondary
                    )
                }

                if (isBatteryOptExempt) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onDisableBatteryOpt,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("disable_battery_opt_prompt_btn")
                    ) {
                        Text(
                            text = "Disable",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (!hasNotificationPermission) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "4. Persistent Lockdown Notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Shows remaining lock time in status bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }

                    Button(
                        onClick = onEnableNotifications,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("enable_notifications_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Allow",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard(
    isExempt: Boolean,
    onRequestExemption: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExempt) EmeraldShield.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isExempt) EmeraldShield.copy(alpha = 0.2f)
                                else AmberWarning.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isExempt) EmeraldShield else AmberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isExempt) "Unrestricted Background (Active)" else "Battery Saver May Kill App",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExempt) EmeraldShield else AmberWarning
                        )
                    }
                }

                if (isExempt) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldShield.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "EXEMPT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestExemption,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("disable_battery_opt_button")
                    ) {
                        Text(
                            text = "Disable",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Android battery savers put background apps to sleep. Disabling battery optimization guarantees ShieldBlock's accessibility scanner never sleeps and remains 100% active.",
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary
            )
        }
    }
}

@Composable
fun AllProtectionsActiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldShield.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldShield,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "System Permissions Granted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Accessibility, Overlay, Notifications, and Unrestricted Background are active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldShield
                )
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    } catch (_: Exception) {
    }
}
