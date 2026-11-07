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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import com.example.ui.components.WeeklyActivityCard
import com.example.ui.focus.SmartFocusDashboardCard
import com.example.ui.i18n.AppStrings.tr
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
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
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
    sectionRules: List<SectionBlockRule> = emptyList(),
    focusSlots: List<FocusScheduleSlot> = emptyList(),
    isSmartFocusEnabled: Boolean = true,
    isBiometricLockEnabled: Boolean = false,
    isBlackoutPrivacyShieldEnabled: Boolean = false,
    preferences: ShieldPreferences? = null,
    usageRefreshTrigger: Long = 0L,
    onToggleProtection: (Boolean) -> Unit,
    onChangeDuration: (Int) -> Unit,
    onToggleStrictMode: (Boolean) -> Unit,
    onToggleSectionRule: (SectionBlockRule) -> Unit = {},
    onAddSectionRule: (SectionBlockRule) -> Unit = {},
    onToggleSmartFocusMaster: (Boolean) -> Unit = {},
    onToggleFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onAddFocusSlot: (FocusScheduleSlot) -> Unit = {},
    onStartFocusSessionNow: (title: String, durationMinutes: Int) -> Unit = { _, _ -> },
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    onToggleBlackoutShield: (Boolean) -> Unit = {},
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
        // App Title & Master Shield Switch (X Blocker Hero)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
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
                                    text = tr("X Blocker", "ایکس بلاکر"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isProtectionEnabled) EmeraldShield.copy(alpha = 0.2f)
                                    else CrimsonPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isProtectionEnabled) tr("ACTIVE", "فعال") else tr("PAUSED", "متوقف"),
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
                                text = if (isProtectionEnabled) {
                                    tr("Real-time Content & Section Guard Active", "محافظ هوشمند محتوا و بخش‌ها فعال است")
                                } else {
                                    tr("Protection Paused — Tap to Resume", "محافظت متوقف است — برای فعال‌سازی ضربه بزنید")
                                },
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
            border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.85f))
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
                                    text = tr("Emergency Panic Mode (SOS)", "حالت اضطراری پنیک (SOS)"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr("Instant self-lockdown & 4-7-8 urge relief", "قفل فوری خودکار و تمرین تنفس ضد وسوسه"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonPrimary
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
                                text = tr("PANIC", "پنیک"),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Always-On Notification Bar SOS Indicator
                    var notificationFeedbackBanner by remember { mutableStateOf<String?>(null) }
                    Surface(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val posted = com.example.notification.LockdownNotificationManager.showOrUpdateAlwaysOnPanicNotification(context)
                                notificationFeedbackBanner = if (posted) {
                                    tr(
                                        "Emergency Panic SOS notification is pinned & active in your Notification Bar!",
                                        "اعلان اضطراری پنیک در نوار اعلان گوشی شما فعال و سنجاق شد!"
                                    )
                                } else {
                                    tr(
                                        "Enable Notification permission below so the Panic SOS bar can appear.",
                                        "لطفاً دسترسی اعلان (Notifications) را فعال کنید."
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianCard.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (hasNotificationPermission) EmeraldShield.copy(alpha = 0.5f) else AmberWarning),
                        modifier = Modifier.testTag("dashboard_pin_notification_bar_button")
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
                                    tint = if (hasNotificationPermission) EmeraldShield else AmberWarning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasNotificationPermission) {
                                        tr("Panic SOS button pinned in Notification Bar (Tap to sync)", "دکمه اضطراری در نوار اعلان فعال است (برای بروزرسانی ضربه بزنید)")
                                    } else {
                                        tr("Tap here to allow Panic SOS Notification Bar", "برای فعال‌سازی دکمه پنیک در نوار اعلان اینجا بزنید")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianTextSecondary
                                )
                            }
                            Text(
                                text = if (hasNotificationPermission) tr("ALWAYS ON", "همیشه فعال") else tr("ALLOW", "فعال‌سازی"),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (hasNotificationPermission) EmeraldShield else AmberWarning,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    notificationFeedbackBanner?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldShield
                        )
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
                title = tr("CLEAN STREAK", "روزهای پاک"),
                value = "${streakDays}d",
                subtitle = tr("Don't break chain", "زنجیره را نشکن"),
                accentColor = EmeraldShield,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = tr("BLOCKS", "مسدودسازی"),
                value = "$incidentCount",
                subtitle = tr("Triggers stopped", "موارد متوقف‌شده"),
                accentColor = CrimsonPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = tr("ACTIVE RULES", "قوانین فعال"),
                value = "$totalKeywordsCount",
                subtitle = tr("EN + FA shield", "محافظ فارسی/انگلیسی"),
                accentColor = CyanTelemetry,
                modifier = Modifier.weight(1f)
            )
        }

        // Motivational "Don't Break the Chain" & Famous Quotes Banner
        com.example.ui.components.MotivationalQuoteBannerCard(streakDays = streakDays)

        // MIDDLE MENU: Dedicated Instagram Reels & Telegram Search Box Section Blocker (Stop Section Only!)
        MiddleSectionBlockerCard(
            sectionRules = sectionRules,
            onToggleSectionRule = onToggleSectionRule,
            onAddSectionRule = onAddSectionRule
        )

        // 7-Day Weekly Activity Status Bento Card (No predefined example apps)
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

        // Biometric Lodge & Simplified Anti-Agent Screenshot Blocker (FLAG_SECURE)
        BiometricAndPrivacyDashboardSection(
            preferences = effectivePrefs,
            isBiometricLockEnabled = isBiometricLockEnabled,
            isBlackoutPrivacyShieldEnabled = isBlackoutPrivacyShieldEnabled,
            onToggleBiometricLock = onToggleBiometricLock,
            onLockAppNow = onLockAppNow,
            onToggleBlackoutShield = onToggleBlackoutShield
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
            border = BorderStroke(1.dp, ObsidianBorder)
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
                            text = tr("Strict Lock Mode", "حالت قفل سخت‌گیرانه"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr("Cannot bypass or dismiss until timer reaches 0", "عدم امکان دور زدن یا بستن قفل تا پایان زمان"),
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
                text = tr(
                    "100% On-Device Protection. Your screen text is never transmitted or saved.",
                    "محافظت ۱۰۰٪ روی دستگاه شما. هیچ متنی از صفحه شما به بیرون ارسال یا ذخیره نمی‌شود."
                ),
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianTextSecondary
            )
        }
    }

    if (pendingAntiTamperAction != null) {
        StrictModeCommitmentDialog(
            actionTitle = when (pendingAntiTamperAction) {
                PendingAntiTamperAction.DISABLE_MASTER_SHIELD -> tr("Pause Master Shield Protection?", "توقف محافظ اصلی برنامه؟")
                PendingAntiTamperAction.DISABLE_STRICT_MODE -> tr("Disable Strict Lock Mode?", "غیرفعال کردن قفل سخت‌گیرانه؟")
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

/**
 * Middle Menu Card on the Dashboard for stopping ONLY Instagram Reels or ONLY the Telegram Search Box
 * without blocking the entire Instagram or Telegram app!
 */
@Composable
fun MiddleSectionBlockerCard(
    sectionRules: List<SectionBlockRule>,
    onToggleSectionRule: (SectionBlockRule) -> Unit,
    onAddSectionRule: (SectionBlockRule) -> Unit,
    modifier: Modifier = Modifier
) {
    val instagramReelsRule = sectionRules.find {
        it.targetPackage.contains("instagram", ignoreCase = true) ||
            it.sectionName.contains("reels", ignoreCase = true)
    }
    val telegramSearchRule = sectionRules.find {
        it.targetPackage.contains("telegram", ignoreCase = true) ||
            it.sectionName.contains("telegram", ignoreCase = true)
    }

    val isInstagramReelsEnabled = instagramReelsRule?.isEnabled ?: true
    val isTelegramSearchEnabled = telegramSearchRule?.isEnabled ?: true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("middle_section_blocker_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr(
                                "In-App Section Guard (Stop Section Only)",
                                "مسدودساز بخش‌های خاص (توقف فقط همان بخش)"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Does NOT block the whole app — only exits Reels or Search Box when entered!",
                                "کل برنامه را نمی‌بندد — فقط هنگام ورود به ریلز یا باکس جستجو از آن بخش خارج می‌شود!"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            }

            // Option 1: Instagram Reels Section Only
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (isInstagramReelsEnabled) EmeraldShield.copy(alpha = 0.5f) else ObsidianBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (isInstagramReelsEnabled) EmeraldShield else ObsidianTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "1. Stop Instagram Reels Section Only",
                                    "۱. توقف فقط بخش Reels اینستاگرام"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr(
                                    "Instagram DMs & Feed work normally; stops ONLY when you enter Reels tab.",
                                    "چت و فید اینستاگرام عادی کار می‌کند؛ فقط با ورود به تب Reels متوقف می‌شود."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isInstagramReelsEnabled,
                        onCheckedChange = {
                            if (instagramReelsRule != null) {
                                onToggleSectionRule(instagramReelsRule)
                            } else {
                                onAddSectionRule(
                                    SectionBlockRule(
                                        targetPackage = "com.instagram.android",
                                        sectionName = "Instagram Reels & Explore",
                                        triggerPattern = "reels|ریلز|clips_viewer",
                                        viewIdPattern = "clips_viewer|reel_viewer|clips_video_container|clips_tab",
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("middle_instagram_reels_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldShield
                        )
                    )
                }
            }

            // Option 2: Telegram Search Box Only
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianCard,
                border = BorderStroke(
                    1.dp,
                    if (isTelegramSearchEnabled) CyanTelemetry.copy(alpha = 0.5f) else ObsidianBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = if (isTelegramSearchEnabled) CyanTelemetry else ObsidianTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(
                                    "2. Stop Telegram Search Box Only",
                                    "۲. توقف فقط باکس جستجوی تلگرام (Search Box)"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr(
                                    "Telegram chats work normally; stops ONLY when you tap the Search box.",
                                    "چت‌های تلگرام عادی کار می‌کنند؛ فقط هنگام ورود به باکس جستجو متوقف می‌شود."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = ObsidianTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isTelegramSearchEnabled,
                        onCheckedChange = {
                            if (telegramSearchRule != null) {
                                onToggleSectionRule(telegramSearchRule)
                            } else {
                                onAddSectionRule(
                                    SectionBlockRule(
                                        targetPackage = "org.telegram.messenger",
                                        sectionName = "Telegram Search Box",
                                        triggerPattern = "search|جستجو|global search|جستجوی جهانی|search messages",
                                        viewIdPattern = "search_src_text|chat_search|search_field",
                                        isEnabled = true
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("middle_telegram_search_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanTelemetry
                        )
                    )
                }
            }
        }
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
                    color = ObsidianTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = tr(
                        "Strict Mode is active to protect your clean streak during moments of urge. To confirm this change, type the phrase below:",
                        "حالت سخت‌گیرانه برای محافظت از رکورد پاک شما فعال است. برای تایید، عبارت زیر را تایپ کنید:"
                    ),
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
                    placeholder = { Text(tr("Type phrase to unlock...", "عبارت را وارد کنید...")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("strict_mode_confirm_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = ObsidianTextPrimary,
                        unfocusedTextColor = ObsidianTextPrimary
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
                Text(tr("Confirm Disable", "تایید غیرفعال‌سازی"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Keep Protected", "ادامه محافظت"), color = EmeraldShield, fontWeight = FontWeight.Bold)
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
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
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
        border = BorderStroke(1.dp, ObsidianBorder)
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
                        text = tr("Lock Duration", "مدت زمان قفل"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CrimsonPrimary)
                ) {
                    Text(
                        text = tr("$currentMinutes min", "$currentMinutes دقیقه"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = tr(
                    "When explicit content is detected, the phone will stay locked for this duration:",
                    "هنگام تشخیص محتوای نامناسب، گوشی به این مدت قفل خواهد شد:"
                ),
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
                        border = if (isSelected) null else BorderStroke(1.dp, ObsidianBorder)
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
                text = tr("Custom Duration Slider (1 to 120 minutes):", "تنظیم دستی زمان قفل (۱ تا ۱۲۰ دقیقه):"),
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
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
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
                    text = tr("Permission Setup Required", "نیاز به فعال‌سازی دسترسی‌ها"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
            }

            Text(
                text = tr(
                    "To monitor on-screen text across browsers, stop Instagram Reels / Telegram Search, and lock over apps, X Blocker needs:",
                    "برای پایش متن صفحه، توقف ریلز اینستاگرام و جستجوی تلگرام، ایکس بلاکر به دسترسی‌های زیر نیاز دارد:"
                ),
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
                        text = tr("1. Accessibility Service", "۱. سرویس دسترسی‌پذیری (Accessibility)"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (isAccessibilityEnabled) tr("Active & scanning", "فعال و در حال پایش") else tr("Needed to detect explicit words & sections", "لازم برای تشخیص کلمات و بخش‌ها"),
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
                            text = tr("Enable", "فعال‌سازی"),
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
                        text = tr("2. Display Over Other Apps", "۲. نمایش روی سایر برنامه‌ها"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (hasOverlayPermission) tr("Active & granted", "فعال") else tr("Required to display lockdown screen", "لازم برای نمایش صفحه قفل"),
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
                            text = tr("Grant", "اعطا"),
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
                        text = tr("3. Stop Battery Optimization", "۳. لغو بهینه‌سازی باتری"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (isBatteryOptExempt) tr("Unrestricted background active", "فعال در پس‌زمینه") else tr("Prevents Android from killing the scanner", "جلوگیری از بسته شدن خودکار برنامه"),
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
                            text = tr("Disable", "لغو"),
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
                            text = tr("4. Persistent Lockdown Notification", "۴. اعلان دائمی قفل"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr("Shows remaining lock time in status bar", "نمایش زمان باقی‌مانده قفل در نوار اعلان"),
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
                            text = tr("Allow", "اجازه"),
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
        border = BorderStroke(
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
                            text = tr("Battery Optimization", "بهینه‌سازی باتری"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = if (isExempt) tr("Unrestricted Background (Active)", "بدون محدودیت در پس‌زمینه (فعال)") else tr("Battery Saver May Kill App", "ذخیره‌ساز باتری ممکن است برنامه را ببندد"),
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
                            text = tr("EXEMPT", "معاف"),
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
                            text = tr("Disable", "لغو"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tr(
                    "Disabling battery optimization guarantees X Blocker's accessibility scanner never sleeps and remains 100% active.",
                    "غیرفعال کردن بهینه‌سازی باتری تضمین می‌کند سرویس ایکس بلاکر در پس‌زمینه همیشه فعال بماند."
                ),
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
        border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
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
                    text = tr("System Permissions Granted", "تمامی دسترسی‌های سیستم فعال هستند"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
                Text(
                    text = tr(
                        "Accessibility, Overlay, Notifications, and Unrestricted Background are active.",
                        "سرویس دسترسی‌پذیری، نمایش روی برنامه‌ها و اجرای پس‌زمینه فعال هستند."
                    ),
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
