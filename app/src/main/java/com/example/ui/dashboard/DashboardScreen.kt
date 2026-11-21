package com.example.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppUsageLimit
import com.example.data.model.BlockIncident
import com.example.data.model.FocusScheduleSlot
import com.example.data.model.SectionBlockRule
import com.example.data.preferences.ShieldPreferences
import com.example.data.preferences.UninstallGuard
import com.example.deviceowner.DeviceOwnerManager
import com.example.deviceowner.DeviceOwnerPolicy
import com.example.deviceowner.ShieldDeviceAdminReceiver
import com.example.notification.LockdownNotificationManager
import com.example.ui.components.MotivationalQuoteBannerCard
import com.example.ui.components.SecureTypeField
import com.example.ui.components.WeeklyActivityCard
import com.example.ui.focus.SmartFocusDashboardCard
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.security.PinAndPrivacyDashboardSection
import com.example.ui.security.RestrictedSettingsGuideDialog
import com.example.ui.streak.NoBeepYoutubeResetStreakDialog
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
import com.example.service.PerAppFirewall
import com.example.util.AppPackageResolver
import com.example.util.DataUsageBudget
import com.example.util.JalaliCalendar
import com.example.util.PerAppInternetRule
import com.example.util.ResolvedAppInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class PendingAntiTamperAction {
    DISABLE_MASTER_SHIELD,
    DISABLE_STRICT_MODE,
    DISABLE_PREVENT_UNINSTALL
}

/**
 * Bilingual duration phrase for the uninstall wait (#40/#41): the length
 * the user picked during first setup (default 1 hour), e.g. "1 hour" /
 * "۱ ساعت", "30 minutes" / "۳۰ دقیقه".
 */
private fun uninstallWaitLabel(minutes: Int): String = when {
    minutes == 60 -> tr("1 hour", "۱ ساعت")
    minutes % 60 == 0 -> tr(
        "${minutes / 60} hours",
        "${JalaliCalendar.toPersianDigits((minutes / 60).toString())} ساعت"
    )
    else -> tr(
        "$minutes minutes",
        "${JalaliCalendar.toPersianDigits(minutes.toString())} دقیقه"
    )
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
    onLockAppNow: () -> Unit = {},
    onToggleBlackoutShield: (Boolean) -> Unit = {},
    onOpenTestSandbox: () -> Unit = {},
    onOpenPanicMode: () -> Unit = {},
    onOpenAddictionQuiz: () -> Unit = {},
    onResetAppFromScratch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val effectivePrefs = remember(context, preferences) {
        preferences ?: ShieldPreferences(context)
    }

    val isPreventUninstallEnabled by effectivePrefs.isPreventUninstallEnabled.collectAsStateWithLifecycle()
    val isInstallGuardEnabled by effectivePrefs.isInstallGuardEnabled.collectAsStateWithLifecycle()
    // Batch 2: the Clear Data guard (#36), the VPN setup block (#42)
    // and the Force Stop guard (#45) have their own toggles, independent
    // of the other guards.
    val isClearDataGuardEnabled by effectivePrefs.isClearDataGuardEnabled.collectAsStateWithLifecycle()
    val isVpnConfigBlockEnabled by effectivePrefs.isVpnConfigBlockEnabled.collectAsStateWithLifecycle()
    val isForceStopGuardEnabled by effectivePrefs.isForceStopGuardEnabled.collectAsStateWithLifecycle()
    val uninstallWaitMinutes by effectivePrefs.uninstallWaitMinutes.collectAsStateWithLifecycle()
    val isDeviceOwnerProtectionEnabled by effectivePrefs.isDeviceOwnerProtectionEnabled.collectAsStateWithLifecycle()
    // Device Owner standing (#11) is system state, not a preference: read
    // it live and refresh on every ON_RESUME (see checkPermissions), so
    // the card flips to ACTIVE the moment provisioning has happened.
    var deviceOwnerStatus by remember {
        mutableStateOf(DeviceOwnerManager.status(context))
    }
    val bestChainDays by effectivePrefs.longestStreakDays.collectAsStateWithLifecycle()
    val quizScorePercent by effectivePrefs.quizScorePercent.collectAsStateWithLifecycle()
    val quizSeverityEn by effectivePrefs.quizSeverityEn.collectAsStateWithLifecycle()
    val quizSeverityFa by effectivePrefs.quizSeverityFa.collectAsStateWithLifecycle()

    var isAccessibilityEnabled by remember {
        mutableStateOf(ShieldPreferences.isAccessibilityServiceEnabled(context))
    }
    var isBatteryOptExempt by remember {
        mutableStateOf(ShieldPreferences.isIgnoringBatteryOptimizations(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    // The master shield can only be enabled after ALL required system
    // permissions are granted (Accessibility, Battery, Notifications).
    // "Display over other apps" is NOT required (#25): the block screen
    // is a normal Activity, nothing draws overlay windows, and the app
    // no longer declares SYSTEM_ALERT_WINDOW.
    val allPermissionsGranted = isAccessibilityEnabled && isBatteryOptExempt && hasNotificationPermission
    var showPermissionsNeededHint by remember { mutableStateOf(false) }

    // Uninstall Guard: a persisted wall-clock deadline; a 1s ticker keeps
    // the countdown text fresh while this screen is open. Since batch 2
    // (#35) the master switch no longer consults this deadline — it only
    // drives the Prevent Uninstall section's own countdown below.
    val uninstallUnlockDeadline by effectivePrefs.uninstallUnlockDeadline.collectAsStateWithLifecycle()
    var uninstallNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            uninstallNowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val uninstallUnlockedNow = UninstallGuard.isUnlocked(uninstallUnlockDeadline, uninstallNowMs)

    var pendingAntiTamperAction by remember { mutableStateOf<PendingAntiTamperAction?>(null) }
    var showHomeResetStreakYoutubeDialog by remember { mutableStateOf(false) }

    // Structured Material Design 3 Collapsible Dropdown States (keeps Home Screen short & clean!)
    var expandSectionGuardDropdown by remember { mutableStateOf(false) }
    var expandUninstallAndLockDropdown by remember { mutableStateOf(false) }
    var expandSmartFocusDropdown by remember { mutableStateOf(false) }
    var expandSecurityAndPermissionsDropdown by remember {
        mutableStateOf(!isAccessibilityEnabled)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            // #52 (batch 4): granting from inside the app arms the
            // daily streak alarm chain right away (it is also
            // (re)scheduled at every app start and boot regardless —
            // posting itself checks the permission and skips silently
            // while it is not granted).
            LockdownNotificationManager.scheduleDailyStreakAlarm(context)
        }
    }

    // #51 (batch 4): the Block VPN toggle's knockdown needs the system
    // VPN consent for X Blocker itself. It is requested the first time
    // the toggle is turned on; a denial keeps the toggle OFF and shows
    // the note on the VPN card.
    var vpnConsentDenied by remember { mutableStateOf(false) }
    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vpnConsentDenied = false
            effectivePrefs.setVpnConfigBlockEnabled(true)
            DeviceOwnerManager.reconcile(context)
        } else {
            vpnConsentDenied = true
        }
    }

    val checkPermissions = {
        isAccessibilityEnabled = ShieldPreferences.isAccessibilityServiceEnabled(context)
        isBatteryOptExempt = ShieldPreferences.isIgnoringBatteryOptimizations(context)
        hasNotificationPermission = checkNotificationPermission(context)
        deviceOwnerStatus = DeviceOwnerManager.status(context)
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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Compact Material Design 3 Hero Card (Master Shield + Best Chain)
        // Liquid Glass: translucent surface so the ambient glows shine through.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = ObsidianSurface.copy(alpha = 0.78f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isProtectionEnabled) listOf(
                                EmeraldShield.copy(alpha = 0.16f),
                                CyanTelemetry.copy(alpha = 0.08f),
                                Color.Transparent
                            ) else listOf(
                                CrimsonPrimary.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        if (isProtectionEnabled) EmeraldShield.copy(alpha = 0.45f) else CrimsonPrimary.copy(alpha = 0.55f),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
                                contentDescription = "usafe Minimal Icon",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .border(
                                        1.5.dp,
                                        if (isProtectionEnabled) EmeraldShield else CrimsonPrimary,
                                        RoundedCornerShape(15.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tr("usafe", "usafe"),
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
                                            text = if (isProtectionEnabled) tr("ACTIVE", "فعال") else tr("OFF", "خاموش"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProtectionEnabled) EmeraldShield else CrimsonPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isProtectionEnabled) {
                                        tr("M3 Content, Reels & Search Guard On", "محافظ هوشمند محتوا، ریلز و جستجو فعال است")
                                    } else {
                                        tr("Protection is Off — Turn On to Activate", "محافظ خاموش است — برای فعال‌سازی روشن کنید")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { targetEnabled ->
                                if (targetEnabled && !allPermissionsGranted) {
                                    // Can't enable until every permission is granted:
                                    // keep the shield off and open the System Setup
                                    // section so the user can grant what's missing.
                                    showPermissionsNeededHint = true
                                    expandSecurityAndPermissionsDropdown = true
                                } else if (!targetEnabled && (strictModeEnabled || isPreventUninstallEnabled)) {
                                    pendingAntiTamperAction = PendingAntiTamperAction.DISABLE_MASTER_SHIELD
                                } else {
                                    showPermissionsNeededHint = false
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

                    if (showPermissionsNeededHint && !allPermissionsGranted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberWarning.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AmberWarning),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("permissions_needed_hint")
                        ) {
                            Text(
                                text = tr(
                                    "Protection can't turn on yet — grant all permissions in System Setup below first (Accessibility, Overlay, Battery, Notifications).",
                                    "محافظ هنوز فعال نمی‌شود — ابتدا همه دسترسی‌ها را در بخش تنظیمات سیستم در پایین بدهید (دسترسی‌پذیری، نمایش روی برنامه‌ها، باتری و اعلان‌ها)."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Compact 3-Pill Telemetry Strip (Clean Streak, Best Chain, Active Rules)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactMetricPill(
                            label = tr("STREAK", "زنجیره"),
                            value = "${streakDays}d",
                            color = EmeraldShield,
                            modifier = Modifier.weight(1f)
                        )
                        CompactMetricPill(
                            label = tr("BEST", "بهترین"),
                            value = "${maxOf(streakDays, bestChainDays)}d",
                            color = CyanTelemetry,
                            modifier = Modifier.weight(1f)
                        )
                        CompactMetricPill(
                            label = tr("RULES", "قوانین"),
                            value = "$totalKeywordsCount",
                            color = IndigoAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Prominent "I Relapsed / Reset Streak" Button right on the Main Hero Card
                    OutlinedButton(
                        onClick = { showHomeResetStreakYoutubeDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("hero_i_relapsed_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("I Relapsed • Reset Streak", "لغزش داشتم • ریست زنجیره"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                    }
                }
            }
        }

        // 2. Compact Action Strip: Emergency Panic Mode (SOS) + Scientific Addiction Quiz
        // Batch #3: both cards share one intrinsic height so they always align.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clickable { onOpenPanicMode() }
                    .testTag("dashboard_panic_mode_card"),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CrimsonPrimary.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CrimsonPrimary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = "Panic SOS",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tr("Panic SOS", "پنیک اضطراری"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = ObsidianTextPrimary
                            )
                            Text(
                                text = tr("Instant Lock & Breathe", "قفل فوری و تنفس"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dashboard_panic_button")
                    ) {
                        // Batch #3: the panic button is a "Breathe" button now.
                        Text(
                            text = tr("Breathe", "تنفس"),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Scientific Addiction Quiz Quick Card (Item 16)
            Card(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clickable { onOpenAddictionQuiz() }
                    .testTag("dashboard_scientific_quiz_card"),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndigoAccent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Scientific Quiz",
                            tint = CyanTelemetry,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (quizScorePercent >= 0) {
                                tr("Quiz: $quizScorePercent%", "آزمون: $quizScorePercent٪")
                            } else {
                                tr("Addiction Quiz", "آزمون وابستگی")
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = if (quizScorePercent >= 0) {
                                tr(quizSeverityEn, quizSeverityFa)
                            } else {
                                tr("Take NoBeep Test", "تست علمی نوبیپ")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = CyanTelemetry,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 3. Duolingo-Style 7-Day Clean Streak Calendar (Item 5)
        WeeklyActivityCard(
            incidents = incidents,
            appLimits = appLimits,
            preferences = effectivePrefs,
            usageRefreshTrigger = usageRefreshTrigger
        )

        // 3b. Reset My Streak at Home Page (opens the direct streak reset dialog)
        Surface(
            onClick = { showHomeResetStreakYoutubeDialog = true },
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_reset_streak_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Streak",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    // Batch #4b: subtitle under the title removed.
                    Text(
                        text = tr("Reset My Streak (I Slipped)", "ریست روزهای پاک (لغزش داشتم)"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                }

                // Batch #4c: the Reset action is a bigger, solid RED button.
                Button(
                    onClick = { showHomeResetStreakYoutubeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("dashboard_reset_streak_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("Reset", "ریست"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. Daily Motivational Quote of the Day (Item 10: Non-tappable, 1 new quote each day)
        MotivationalQuoteBannerCard(streakDays = streakDays)

        // =========================================================================
        // STRUCTURED MATERIAL DESIGN 3 DROPDOWN SECTIONS (Item 6: Shorter Home Screen)
        // =========================================================================

        // DROPDOWN 1: In-App Section Guard (Instagram Reels & Telegram Search Box)
        val activeSectionsCount = sectionRules.count { it.isEnabled }
        M3CollapsibleDropdownSection(
            title = tr(
                "In-App Section Blocker (Reels & Search)",
                "مسدودساز بخش‌های برنامه (ریلز و جستجوی تلگرام)"
            ),
            subtitle = tr(
                "Shows Full Block Page on Instagram Reels & Telegram Search (Archived Chats safe)",
                "نمایش صفحه قفل کامل در ریلز و جستجوی تلگرام (بدون مسدودسازی آرشیو)"
            ),
            badgeText = tr("$activeSectionsCount Active", "$activeSectionsCount فعال"),
            icon = Icons.Default.VisibilityOff,
            accentColor = CrimsonPrimary,
            expanded = expandSectionGuardDropdown,
            onToggleExpand = { expandSectionGuardDropdown = !expandSectionGuardDropdown },
            testTag = "dropdown_section_blocker"
        ) {
            MiddleSectionBlockerCard(
                sectionRules = sectionRules,
                onToggleSectionRule = onToggleSectionRule,
                onAddSectionRule = onAddSectionRule
            )
        }

        // DROPDOWN 2: Prevent Uninstall, Strict Lock Mode & Lock Duration (Items 7 & 8)
        M3CollapsibleDropdownSection(
            title = tr(
                "Prevent Uninstall & Lock Timer",
                "محافظ ضدحذف برنامه و تنظیمات زمان قفل"
            ),
            subtitle = tr(
                "Requires the Long Motivational Sentence to turn off",
                "نیازمند نوشتن جمله انگیزشی طولانی برای خاموش کردن"
            ),
            badgeText = if (isPreventUninstallEnabled) {
                tr("Protected • ${lockDurationMinutes}m", "محافظت‌شده • ${lockDurationMinutes}m")
            } else {
                "${lockDurationMinutes}m"
            },
            icon = Icons.Default.Lock,
            accentColor = AmberWarning,
            expanded = expandUninstallAndLockDropdown,
            onToggleExpand = { expandUninstallAndLockDropdown = !expandUninstallAndLockDropdown },
            testTag = "dropdown_prevent_uninstall_and_lock"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Prevent Uninstall Guard Card (Item 7)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPreventUninstallEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldShield.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr("Prevent Uninstall Protection", "محافظ جلوگیری از حذف برنامه"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Blocks uninstalling usafe. Turning off requires typing a long motivational sentence.",
                                        "مانع از حذف usafe می‌شود. خاموش کردن نیازمند نوشتن جمله انگیزشی طولانی است."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isPreventUninstallEnabled,
                            onCheckedChange = { targetEnabled ->
                                if (!targetEnabled) {
                                    pendingAntiTamperAction = PendingAntiTamperAction.DISABLE_PREVENT_UNINSTALL
                                } else {
                                    effectivePrefs.setPreventUninstallEnabled(true)
                                }
                            },
                            modifier = Modifier.testTag("prevent_uninstall_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldShield
                            )
                        )
                    }
                }

                // Install Guard Card (#27): new app installs need a PIN
                // confirmation within 10 seconds, even from Google Play.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isInstallGuardEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldShield.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr("Install Guard", "محافظ نصب برنامه"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Installing a new app (Play Store or APK) asks for your PIN. No PIN within 10 seconds = install blocked.",
                                        "نصب برنامهٔ جدید (پلی‌استور یا APK) پین می‌خواهد. بدون پین ظرف ۱۰ ثانیه = نصب مسدود است."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isInstallGuardEnabled,
                            onCheckedChange = { targetEnabled ->
                                effectivePrefs.setInstallGuardEnabled(targetEnabled)
                            },
                            modifier = Modifier.testTag("install_guard_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldShield
                            )
                        )
                    }
                }

                // Clear Data Guard Card (#36, batch 2): its OWN switch —
                // independent of Prevent Uninstall and of the master
                // switch — intercepting "Clear data" on this app's
                // App-info page so the app cannot be wiped from Settings.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isClearDataGuardEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldShield.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr("Prevent Clear Data", "محافظ جلوگیری از پاک کردن داده‌ها"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Blocks 'Clear data' on usafe's App info page, so the app cannot be wiped from Settings. Its own switch — works even while the master switch is off.",
                                        "«پاک کردن داده‌ها» در صفحه اطلاعات usafe را مسدود می‌کند تا برنامه از تنظیمات پاک نشود. کلید جدا دارد — حتی وقتی محافظ اصلی خاموش است هم کار می‌کند."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isClearDataGuardEnabled,
                            onCheckedChange = { targetEnabled ->
                                effectivePrefs.setClearDataGuardEnabled(targetEnabled)
                            },
                            modifier = Modifier.testTag("clear_data_guard_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldShield
                            )
                        )
                    }
                }

                // VPN Setup Block Card (#42, batch 2): a VPN would bypass
                // the blocker, so configuring one is prevented. With
                // Device Owner this is a hard system restriction
                // (DISALLOW_CONFIG_VPN); without it, VPN settings screens
                // and consent dialogs are interrupted by the block screen.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isVpnConfigBlockEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldShield.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr("Block VPN Setup", "مسدود کردن تنظیم VPN"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Prevents setting up a VPN on this phone, since a VPN can bypass the blocker. Hard system block with Device Owner; otherwise VPN settings pages are interrupted.",
                                        "از تنظیم VPN روی این گوشی جلوگیری می‌کند، چون VPN می‌تواند مسدودساز را دور بزند. با مالک دستگاه (Device Owner) مسدودسازی سیستمی و قطعی است؛ در غیر این صورت صفحه‌های تنظیم VPN قطع می‌شوند."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isVpnConfigBlockEnabled,
                            onCheckedChange = { targetEnabled ->
                                if (targetEnabled) {
                                    // #51 (batch 4): ask for the system
                                    // VPN consent the FIRST time the
                                    // toggle is enabled (the knockdown
                                    // cannot work without it). Once
                                    // granted, prepare() returns null and
                                    // enabling is silent forever after.
                                    // Denied → the toggle stays OFF and
                                    // the card shows why.
                                    val consentIntent = try {
                                        VpnService.prepare(context)
                                    } catch (e: Exception) {
                                        null
                                    }
                                    if (consentIntent == null) {
                                        vpnConsentDenied = false
                                        effectivePrefs.setVpnConfigBlockEnabled(true)
                                        // Apply the Device Owner
                                        // restriction right away; a
                                        // no-op when not Device Owner.
                                        DeviceOwnerManager.reconcile(context)
                                    } else {
                                        vpnConsentLauncher.launch(consentIntent)
                                    }
                                } else {
                                    vpnConsentDenied = false
                                    effectivePrefs.setVpnConfigBlockEnabled(false)
                                    DeviceOwnerManager.reconcile(context)
                                    // #63 (batch 5): the per-app
                                    // firewall lives only while this
                                    // guard is on — stop it at once
                                    // instead of waiting for the next
                                    // evaluation.
                                    PerAppFirewall.requestStop(context)
                                }
                            },
                            modifier = Modifier.testTag("vpn_config_block_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldShield
                            )
                        )
                    }
                    // #51 (batch 4): shown when the one-time VPN consent
                    // was denied — the toggle stayed OFF, and this is why.
                    if (vpnConsentDenied) {
                        Text(
                            text = tr(
                                "VPN permission is required: without it, foreign VPNs cannot be disconnected.",
                                "اجازه‌ی VPN لازمه: بدون اون نمی‌شه VPNهای خارجی رو قطع کرد."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = AmberWarning,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                        )
                    }
                    }
                }

                // Per-App Internet Access Card (#63, batch 5): each
                // app can be blocked on Wi-Fi and/or mobile data and
                // given its own daily volume cap. The section is
                // ACTIVE only while the Block VPN guard above is on
                // (its firewall is usafe's own VPN); the global daily
                // cap always wins over these per-app limits.
                PerAppInternetCard(
                    preferences = effectivePrefs,
                    vpnGuardEnabled = isVpnConfigBlockEnabled
                )

                // Force Stop Guard Card (#45, batch 2): force-stopping
                // X Blocker from Settings kills every guard at once, so
                // its App-info page is guarded. With Device Owner this is
                // a hard system restriction (DISALLOW_APPS_CONTROL);
                // without it, the App-info page is interrupted by the
                // block screen while this switch is on.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isForceStopGuardEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldShield.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tr("Protect from Force Stop", "محافظت در برابر توقف اجباری (Force Stop)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Prevents force-stopping usafe from Settings, which would kill every guard at once. Hard system block with Device Owner; otherwise usafe's App info page is interrupted.",
                                        "از توقف اجباری usafe از تنظیمات جلوگیری می‌کند؛ توقفی که همه محافظ‌ها را یکجا از کار می‌اندازد. با مالک دستگاه (Device Owner) مسدودسازی سیستمی و قطعی است؛ در غیر این صورت صفحه اطلاعات usafe قطع می‌شود."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isForceStopGuardEnabled,
                            onCheckedChange = { targetEnabled ->
                                effectivePrefs.setForceStopGuardEnabled(targetEnabled)
                                // Apply (or clear) the Device Owner
                                // restriction right away; a no-op when
                                // the app is not Device Owner.
                                DeviceOwnerManager.reconcile(context)
                            },
                            modifier = Modifier.testTag("force_stop_guard_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldShield
                            )
                        )
                    }
                }

                // Device Owner Card (#11): the DEFINITIVE block against
                // ADB/computer uninstall. An app cannot make itself Device
                // Owner, so until one-time provisioning has happened this
                // card shows the honest setup steps and the switch stays
                // disabled — it never pretends to work. Once the app IS
                // Device Owner, the switch drives the real DPM blocks
                // (own-package uninstall block + ADB/debugging off),
                // applied by DeviceOwnerManager.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (deviceOwnerStatus == DeviceOwnerPolicy.Status.DEVICE_OWNER &&
                            isDeviceOwnerProtectionEnabled
                        ) {
                            EmeraldShield.copy(alpha = 0.55f)
                        } else {
                            ObsidianBorder
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldShield.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = EmeraldShield,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tr("Device Owner Protection", "محافظ مالک دستگاه"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianTextPrimary
                                    )
                                    Text(
                                        text = tr(
                                            "The hard block, enforced by Android itself: usafe cannot be uninstalled (not even via ADB) and USB debugging stays off.",
                                            "مسدودسازی قطعی با اجرای خود اندروید: usafe حذف نمی‌شود (حتی با ADB) و اشکال‌زدایی USB خاموش می‌ماند."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = ObsidianTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isDeviceOwnerProtectionEnabled,
                                onCheckedChange = { targetEnabled ->
                                    effectivePrefs.setDeviceOwnerProtectionEnabled(targetEnabled)
                                    DeviceOwnerManager.reconcile(context, targetEnabled)
                                },
                                enabled = deviceOwnerStatus == DeviceOwnerPolicy.Status.DEVICE_OWNER,
                                modifier = Modifier.testTag("device_owner_protection_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldShield
                                )
                            )
                        }

                        when (deviceOwnerStatus) {
                            DeviceOwnerPolicy.Status.DEVICE_OWNER -> Text(
                                text = tr(
                                    "Status: ACTIVE — usafe is this phone's Device Owner. The switch above turns the Android-level blocks on and off.",
                                    "وضعیت: فعال — usafe مالک دستگاه این گوشی است. کلید بالا مسدودسازی‌های سطح اندروید را روشن و خاموش می‌کند."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = EmeraldShield
                            )

                            DeviceOwnerPolicy.Status.PROFILE_OWNER_ONLY -> Text(
                                text = tr(
                                    "Status: usafe only owns a work profile — that is not enough for the ADB block. Full Device Owner setup (below) is required.",
                                    "وضعیت: usafe فقط مالک یک پروفایل کاری است — برای مسدودسازی ADB کافی نیست. راه‌اندازی کامل مالک دستگاه (پایین) لازم است."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = AmberWarning
                            )

                            DeviceOwnerPolicy.Status.ADMIN_ONLY,
                            DeviceOwnerPolicy.Status.NOT_PROVISIONED -> Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "Status: NOT SET UP — no app can make itself Device Owner; the switch only starts working after a one-time setup from a computer. Connect the phone with USB debugging ON and run:",
                                        "وضعیت: راه‌اندازی نشده — هیچ برنامه‌ای نمی‌تواند خودش را مالک دستگاه کند؛ کلید فقط بعد از یک راه‌اندازی یک‌باره از رایانه کار می‌کند. گوشی را با اشکال‌زدایی USB روشن وصل کنید و این دستور را بزنید:"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                                Text(
                                    text = DeviceOwnerPolicy.provisioningCommand(
                                        context.packageName,
                                        ShieldDeviceAdminReceiver::class.java.name
                                    ),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = CyanTelemetry
                                )
                                Text(
                                    text = tr(
                                        "⚠ The command FAILS while any Google account is signed in on the phone — remove the accounts first (you can sign back in afterwards). The only other route is QR provisioning during the very first setup screens, right after a factory reset.",
                                        "⚠ تا وقتی هر حساب گوگلی روی گوشی وارد شده باشد این دستور خطا می‌دهد — اول حساب‌ها را حذف کنید (بعدش می‌توانید دوباره وارد شوید). راه دیگر فقط اسکن QR در صفحه‌های راه‌اندازی اولیه، درست بعد از بازنشانی کارخانه است."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }

                // Uninstall Request Card — user-chosen wait (#41; the
                // built-in default is 1 hour since #40, previously 5).
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (uninstallUnlockedNow) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberWarning.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Uninstall usafe (Wait: ${uninstallWaitLabel(uninstallWaitMinutes)})",
                                        "حذف نصب usafe (انتظار: ${uninstallWaitLabel(uninstallWaitMinutes)})"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Deleting the app is deliberately slow: request it here, wait ${uninstallWaitLabel(uninstallWaitMinutes)}, then the uninstall guard stands down and the app can be uninstalled normally.",
                                        "حذف برنامه عمداً کند است: این‌جا درخواست بدهید، ${uninstallWaitLabel(uninstallWaitMinutes)} صبر کنید، بعد محافظ حذف کنار می‌رود و برنامه به‌صورت عادی حذف می‌شود."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }

                        when {
                            uninstallUnlockedNow -> {
                                Text(
                                    text = tr(
                                        "✓ Wait complete — the uninstall guard has stood down: you can now uninstall usafe from system Settings.",
                                        "✓ انتظار تمام شد — محافظ حذف کنار رفت: حالا می‌توانید usafe را از تنظیمات سیستم حذف کنید."
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldShield,
                                    modifier = Modifier.testTag("uninstall_unlocked_text")
                                )
                            }
                            UninstallGuard.isRequested(uninstallUnlockDeadline) -> {
                                Text(
                                    text = tr(
                                        "Uninstall wait running — remaining: " + UninstallGuard.formatRemainingHms(
                                            UninstallGuard.remainingMillis(uninstallUnlockDeadline, uninstallNowMs)
                                        ) + "  (keeps counting even after a reboot)",
                                        "انتظار حذف نصب در حال انجام — باقی‌مانده: " + UninstallGuard.formatRemainingHms(
                                            UninstallGuard.remainingMillis(uninstallUnlockDeadline, uninstallNowMs)
                                        ) + "  (حتی بعد از راه‌اندازی مجدد هم ادامه دارد)"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning,
                                    modifier = Modifier.testTag("uninstall_countdown_text")
                                )
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = { effectivePrefs.requestUninstallUnlock() },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("request_uninstall_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr(
                                            "Request Uninstall — Start the ${uninstallWaitLabel(uninstallWaitMinutes)} Wait",
                                            "درخواست حذف نصب — شروع انتظار ${uninstallWaitLabel(uninstallWaitMinutes)}"
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Strict Lock Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = tr("Strict Lock Mode", "حالت قفل سخت‌گیرانه"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Only Phone Calls & Texts allowed while timer counts down",
                                        "هنگام شمارش تایمر فقط تماس تلفنی و پیامک مجاز است"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
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

                // Lock Duration Picker
                LockDurationCard(
                    currentMinutes = lockDurationMinutes,
                    onChangeDuration = onChangeDuration
                )
            }
        }

        // DROPDOWN 3: Smart Focus Deep Work Schedules
        M3CollapsibleDropdownSection(
            title = tr(
                "Smart Focus Deep Work Schedules",
                "زمان‌بندی تمرکز هوشمند (Smart Focus)"
            ),
            subtitle = tr(
                "Auto-lock phone during study, work, or sleep hours",
                "قفل خودکار گوشی در ساعات مطالعه، کار یا خواب شبانه"
            ),
            badgeText = if (isSmartFocusEnabled) tr("Enabled", "فعال") else tr("Off", "خاموش"),
            icon = Icons.Default.Timer,
            accentColor = CyanTelemetry,
            expanded = expandSmartFocusDropdown,
            onToggleExpand = { expandSmartFocusDropdown = !expandSmartFocusDropdown },
            testTag = "dropdown_smart_focus"
        ) {
            SmartFocusDashboardCard(
                focusSlots = focusSlots,
                isSmartFocusEnabled = isSmartFocusEnabled,
                onToggleSmartFocusMaster = onToggleSmartFocusMaster,
                onToggleFocusSlot = onToggleFocusSlot,
                onAddFocusSlot = onAddFocusSlot,
                onStartFocusSessionNow = onStartFocusSessionNow
            )
        }

        // DROPDOWN 4: Privacy Shield, PIN Lock & System Permissions
        val allPermsReady = allPermissionsGranted
        M3CollapsibleDropdownSection(
            title = tr(
                "Privacy Shield, PIN Lock & System Setup",
                "حریم خصوصی، قفل پین و دسترسی‌های سیستم"
            ),
            subtitle = tr(
                "Screenshot blocker, PIN lock, Accessibility & Notification Bar",
                "مسدودساز اسکرین‌شات، قفل پین، دسترسی‌پذیری و نوار اعلان"
            ),
            badgeText = if (allPermsReady) tr("All Ready", "تکمیل") else tr("Setup", "نیاز به تنظیم"),
            icon = Icons.Default.Security,
            accentColor = if (allPermsReady) EmeraldShield else AmberWarning,
            expanded = expandSecurityAndPermissionsDropdown,
            onToggleExpand = { expandSecurityAndPermissionsDropdown = !expandSecurityAndPermissionsDropdown },
            testTag = "dropdown_security_and_permissions"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PinAndPrivacyDashboardSection(
                    preferences = effectivePrefs,
                    isBlackoutPrivacyShieldEnabled = isBlackoutPrivacyShieldEnabled,
                    onLockAppNow = onLockAppNow,
                    onToggleBlackoutShield = onToggleBlackoutShield
                )

                if (!allPermsReady) {
                    PermissionRequiredCard(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isBatteryOptExempt = isBatteryOptExempt,
                        hasNotificationPermission = hasNotificationPermission,
                        onEnableAccessibility = {
                            openAccessibilitySettings(context)
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

                BatteryOptimizationCard(
                    isExempt = isBatteryOptExempt,
                    onRequestExemption = {
                        ShieldPreferences.requestDisableBatteryOptimization(context)
                        checkPermissions()
                    }
                )
            }
        }
    }

    val activeAntiTamperAction = pendingAntiTamperAction
    if (activeAntiTamperAction != null) {
        MotivationUnlockGateDialog(
            actionTitle = when (activeAntiTamperAction) {
                PendingAntiTamperAction.DISABLE_MASTER_SHIELD -> tr(
                    "Pause Master Shield Protection?",
                    "توقف محافظ اصلی برنامه؟"
                )
                PendingAntiTamperAction.DISABLE_STRICT_MODE -> tr(
                    "Disable Strict Lock Mode?",
                    "غیرفعال کردن قفل سخت‌گیرانه؟"
                )
                PendingAntiTamperAction.DISABLE_PREVENT_UNINSTALL -> tr(
                    "Turn Off Prevent Uninstall?",
                    "خاموش کردن محافظ جلوگیری از حذف؟"
                )
            },
            onConfirmBySentence = {
                when (activeAntiTamperAction) {
                    PendingAntiTamperAction.DISABLE_MASTER_SHIELD -> onToggleProtection(false)
                    PendingAntiTamperAction.DISABLE_STRICT_MODE -> onToggleStrictMode(false)
                    PendingAntiTamperAction.DISABLE_PREVENT_UNINSTALL -> effectivePrefs.setPreventUninstallEnabled(false)
                }
                pendingAntiTamperAction = null
            },
            onDismiss = { pendingAntiTamperAction = null }
        )
    }

    if (showHomeResetStreakYoutubeDialog) {
        NoBeepYoutubeResetStreakDialog(
            bestChainDays = maxOf(streakDays, bestChainDays),
            onConfirmResetStreak = { note ->
                effectivePrefs.resetStreak(note)
            },
            onDismiss = { showHomeResetStreakYoutubeDialog = false }
        )
    }
}

/**
 * #63 (batch 5): the Per-App Internet Access card. Rules are edited
 * here and enforced by usafe's own VPN firewall (see PerAppFirewall)
 * — but ONLY while the Block VPN guard is on: with the guard off the
 * card is inactive and the firewall does not run. The global daily
 * cap (#43/#46) always takes precedence over these per-app limits.
 */
@Composable
private fun PerAppInternetCard(
    preferences: ShieldPreferences,
    vpnGuardEnabled: Boolean
) {
    val rules by preferences.perAppInternetRules.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (vpnGuardEnabled) EmeraldShield.copy(alpha = 0.55f) else ObsidianBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (vpnGuardEnabled) EmeraldShield else ObsidianTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tr("Per-App Internet Access", "دسترسی اینترنت برنامه‌ها"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = tr(
                            "Block an app on Wi-Fi or mobile data, or give it a daily cap.",
                            "اینترنت یک برنامه را روی وای‌فای یا داده موبایل ببندید، یا برایش سقف روزانه بگذارید."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!vpnGuardEnabled) {
                Text(
                    text = tr(
                        "Inactive — this section works only while Block VPN Setup (above) is ON.",
                        "غیرفعال است — این بخش فقط وقتی کار می‌کند که «مسدود کردن تنظیم VPN» (بالا) روشن باشد."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberWarning
                )
            } else {
                Text(
                    text = tr(
                        "The overall daily cap always wins over per-app limits.",
                        "سقف کلی روزانه همیشه بر محدودیت‌های تک‌برنامه‌ای اولویت دارد."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )
            }
            rules.forEach { rule ->
                PerAppRuleRow(
                    rule = rule,
                    enabled = vpnGuardEnabled,
                    onChange = { updated ->
                        preferences.setPerAppInternetRules(
                            rules.map { if (it.packageName == rule.packageName) updated else it }
                        )
                    },
                    onRemove = {
                        preferences.setPerAppInternetRules(rules - rule)
                    }
                )
            }
            if (rules.isEmpty()) {
                Text(
                    text = tr(
                        "No apps added yet.",
                        "هنوز برنامه‌ای اضافه نشده است."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showPicker = true },
                enabled = vpnGuardEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("per_app_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Add app", "افزودن برنامه"), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPicker) {
        PerAppPickerDialog(
            existingPackages = rules.map { it.packageName }.toSet(),
            onPick = { info ->
                preferences.setPerAppInternetRules(
                    rules + PerAppInternetRule(
                        packageName = info.packageName,
                        blockOnWifi = true,
                        blockOnMobile = true
                    )
                )
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/** #63: one app's rule row — name, Wi-Fi / mobile block switches, a
 *  daily-cap slider (same 50 MB–20 GB scale as the global caps),
 *  and remove. */
@Composable
private fun PerAppRuleRow(
    rule: PerAppInternetRule,
    enabled: Boolean,
    onChange: (PerAppInternetRule) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val appLabel = remember(rule.packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationInfo(rule.packageName, 0).loadLabel(pm).toString()
        } catch (_: Exception) {
            rule.packageName
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianTextSecondary
                )
            }
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.testTag("per_app_remove_${rule.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = tr("Remove", "حذف"),
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr("Block on Wi-Fi", "مسدود روی وای‌فای"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Switch(
                checked = rule.blockOnWifi,
                onCheckedChange = { onChange(rule.copy(blockOnWifi = it)) },
                enabled = enabled,
                modifier = Modifier.testTag("per_app_block_wifi_${rule.packageName}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = EmeraldShield
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr("Block on mobile data", "مسدود روی داده موبایل"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Switch(
                checked = rule.blockOnMobile,
                onCheckedChange = { onChange(rule.copy(blockOnMobile = it)) },
                enabled = enabled,
                modifier = Modifier.testTag("per_app_block_mobile_${rule.packageName}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = EmeraldShield
                )
            )
        }
        val capStops = DataUsageBudget.SLIDER_CAPS_MB
        var capIndex by remember(rule.packageName, rule.dailyCapMb) {
            mutableStateOf(DataUsageBudget.sliderIndexFor(rule.dailyCapMb).toFloat())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tr("Daily cap", "سقف روزانه"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = if (rule.dailyCapMb <= 0) {
                    tr("No cap", "بدون سقف")
                } else {
                    DataUsageBudget.formatBytes(DataUsageBudget.mbToBytes(rule.dailyCapMb))
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (rule.dailyCapMb > 0) EmeraldShield else ObsidianTextSecondary,
                modifier = Modifier.testTag("per_app_cap_label_${rule.packageName}")
            )
        }
        Slider(
            value = capIndex,
            onValueChange = { capIndex = it },
            onValueChangeFinished = {
                onChange(
                    rule.copy(
                        dailyCapMb = DataUsageBudget.capForSliderIndex(capIndex.roundToInt())
                    )
                )
            },
            valueRange = 0f..(capStops.size - 1).toFloat(),
            steps = capStops.size - 2,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = EmeraldShield,
                activeTrackColor = EmeraldShield
            ),
            modifier = Modifier.testTag("per_app_cap_slider_${rule.packageName}")
        )
    }
}

/** #63: pick an installed app to add a rule for. */
@Composable
private fun PerAppPickerDialog(
    existingPackages: Set<String>,
    onPick: (ResolvedAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            AppPackageResolver.queryInstalledApps(context)
        }
    }
    val visible = installedApps
        .filter { it.isInstalled && it.packageName != context.packageName }
        .filter { it.packageName !in existingPackages }
        .filter {
            query.isBlank() ||
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
        .sortedBy { it.appName.lowercase() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Add app", "افزودن برنامه"),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            tr("Search apps…", "جستجوی برنامه‌ها…"),
                            color = ObsidianTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ObsidianTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CrimsonPrimary,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    visible.take(80).forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(info) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = info.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = info.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianTextSecondary
                                )
                            }
                        }
                    }
                    if (visible.isEmpty()) {
                        Text(
                            text = tr(
                                "No apps to add.",
                                "برنامه‌ای برای افزودن نیست."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "لغو"), color = CrimsonPrimary)
            }
        },
        containerColor = ObsidianCard
    )
}

@Composable
private fun CompactMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianTextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Material Design 3 Structured Collapsible Dropdown Card (Item 6)
 */
@Composable
fun M3CollapsibleDropdownSection(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dropdown_arrow_rotation"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag(testTag),
        colors = CardDefaults.elevatedCardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (expanded) accentColor.copy(alpha = 0.65f) else ObsidianBorder,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = ObsidianTextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = ObsidianTextSecondary,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * In-App Section Blocker Card for Instagram Reels & Telegram Search Box (Items 3 & 4):
 * Shows Full Block Page for both Instagram Reels and Telegram Search, while ignoring Telegram Archived Chats.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("middle_section_blocker_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Option 1: Instagram Reels Section
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
                                "1. Instagram Reels Blocker (Full Block Page)",
                                "۱. مسدودساز بخش Reels اینستاگرام (صفحه قفل کامل)"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "DMs & Feed work normally; entering Reels opens the usafe Lock Page.",
                                "چت و فید عادی کار می‌کند؛ با ورود به Reels صفحه قفل کامل باز می‌شود."
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

        // Option 2: Telegram Search Box (Archive Safe + Full Block Page)
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
                                "2. Telegram Search Box Blocker (Archive Safe)",
                                "۲. مسدودساز باکس جستجوی تلگرام (ایمن برای آرشیو)"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextPrimary
                        )
                        Text(
                            text = tr(
                                "Chats & Archived Chats work freely; tapping Search opens the Block Page.",
                                "چت‌ها و بخش آرشیو کاملاً باز هستند؛ زدن روی جستجو صفحه قفل را باز می‌کند."
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

/**
 * Unlock Gate Dialog (Items 7 & 8, Confirmed by user):
 * Allows turning off Prevent Uninstall, Strict Mode, or Master Shield by
 * typing a long motivational commitment sentence.
 * (A former alternative — spending 20 coins — was removed together with
 * the coin system at the owner's request; the sentence path is unchanged.)
 */
@Composable
private fun MotivationUnlockGateDialog(
    actionTitle: String,
    onConfirmBySentence: () -> Unit,
    onDismiss: () -> Unit
) {
    val longMotivationalSentence =
        "I am the master of my mind and I refuse to trade my clean streak chain for a temporary urge"
    val legacyShortPhrase = "I choose self control"
    var typedPhrase by remember { mutableStateOf("") }
    val normalizedInput = typedPhrase.trim()
    val isMatched = normalizedInput.equals(longMotivationalSentence, ignoreCase = true) ||
        normalizedInput.equals(legacyShortPhrase, ignoreCase = true)

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(
                        "To turn off protection, type the long motivational sentence below:",
                        "برای خاموش کردن این محافظ، جمله انگیزشی طولانی زیر را کامل بنویسید:"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianTextSecondary
                )

                // Type Long Motivational Sentence
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"$longMotivationalSentence\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CyanTelemetry,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Secure anti-cheat field (#5, #8, #9): FLAG_SECURE while the
                // dialog is open, paste/dictation bulk inserts rejected, and an
                // orange underline marks where the typed text differs from the
                // target sentence so a disabled Confirm is self-explanatory.
                SecureTypeField(
                    value = typedPhrase,
                    onValueChange = { typedPhrase = it },
                    targetSentence = longMotivationalSentence,
                    placeholder = {
                        Text(
                            tr(
                                "Type the full motivational sentence...",
                                "جمله انگیزشی بالا را به صورت کامل تایپ کنید..."
                            )
                        )
                    },
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
                onClick = onConfirmBySentence,
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
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tr("Lock Duration", "مدت زمان قفل"),
                        style = MaterialTheme.typography.titleSmall,
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
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            .height(36.dp)
                            .testTag("preset_${min}_min"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) CrimsonPrimary else ObsidianSurface,
                        border = if (isSelected) null else BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${min}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else ObsidianTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
    isBatteryOptExempt: Boolean,
    hasNotificationPermission: Boolean,
    onEnableAccessibility: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onEnableNotifications: () -> Unit
) {
    // #25: on Android 13+ a sideloaded app cannot enable its accessibility
    // service until the user allows "restricted settings" in App info.
    // The app cannot bypass that system step — it can only explain it.
    var showRestrictedSettingsGuide by remember { mutableStateOf(false) }
    if (showRestrictedSettingsGuide) {
        RestrictedSettingsGuideDialog(onDismiss = { showRestrictedSettingsGuide = false })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Permission Setup Required", "نیاز به فعال‌سازی دسترسی‌ها"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accessibility permission item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("1. Accessibility Service", "۱. سرویس دسترسی‌پذیری"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (isAccessibilityEnabled) tr("Active", "فعال") else tr("Required for Reels/Search & Web guard", "لازم برای محافظت از ریلز، جستجو و وب"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAccessibilityEnabled) EmeraldShield else ObsidianTextSecondary
                    )
                }

                if (isAccessibilityEnabled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(22.dp)
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
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // #25 guide entry: only relevant while the service is still
            // off — that is exactly when Android 13+'s restricted-settings
            // wall blocks sideloaded apps from enabling accessibility.
            if (!isAccessibilityEnabled) {
                TextButton(
                    onClick = { showRestrictedSettingsGuide = true },
                    modifier = Modifier.testTag("restricted_settings_guide_button")
                ) {
                    Text(
                        text = tr(
                            "Can't turn it on? Android may be blocking it — see the fix",
                            "روشن نمی‌شود؟ شاید اندروید جلوی آن را گرفته — راه‌حل را ببینید"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Battery Optimization Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("2. Unrestricted Battery", "۲. لغو بهینه‌سازی باتری"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                }

                if (isBatteryOptExempt) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(22.dp)
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
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // #52 (batch 4): the Notifications row is where the
            // POST_NOTIFICATIONS permission lives now — it is never
            // requested at app launch anymore. The row always renders
            // and shows its state like the rows above: granted (always
            // the case before Android 13) shows Active; otherwise the
            // Allow button fires the runtime request.
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("3. Notifications", "۳. اعلان‌ها"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (hasNotificationPermission) tr("Active", "فعال") else tr("For the daily streak reminder", "برای یادآور روزانه زنجیره"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasNotificationPermission) EmeraldShield else ObsidianTextSecondary
                    )
                }

                if (hasNotificationPermission) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Enabled",
                        tint = EmeraldShield,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Button(
                        onClick = onEnableNotifications,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("enable_notifications_btn")
                    ) {
                        Text(
                            text = tr("Allow", "اجازه"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
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
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isExempt) EmeraldShield.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isExempt) EmeraldShield else AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = tr("Battery Optimization", "بهینه‌سازی باتری"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianTextPrimary
                    )
                    Text(
                        text = if (isExempt) tr("Unrestricted Background Active", "بدون محدودیت در پس‌زمینه") else tr("Tap Disable to keep scanner alive", "برای فعال ماندن دائمی لغو کنید"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExempt) EmeraldShield else AmberWarning
                    )
                }
            }

            if (!isExempt) {
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
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun AllProtectionsActiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldShield,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tr(
                    "All System Permissions Active",
                    "تمامی دسترسی‌های سیستم فعال هستند"
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldShield
            )
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

// No openOverlaySettings() anymore (#25): the app declares no
// SYSTEM_ALERT_WINDOW permission and never asks for it — the block
// screen is a normal Activity, not an overlay window.
