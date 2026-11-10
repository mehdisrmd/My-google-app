package com.example.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
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

data class QuizOption(
    val score: Int,
    val labelEn: String,
    val labelFa: String
)

data class ScientificQuizQuestion(
    val id: Int,
    val dimensionEn: String,
    val dimensionFa: String,
    val questionEn: String,
    val questionFa: String,
    val options: List<QuizOption>
)

object ScientificAddictionQuizRepository {
    private val standardFrequencyOptions = listOf(
        QuizOption(0, "Never / Almost Never", "هرگز / به ندرت"),
        QuizOption(1, "Occasionally (1–2 times a month)", "گاهی اوقات (۱ تا ۲ بار در ماه)"),
        QuizOption(2, "Frequently (Several times a week)", "اغلب اوقات (چند بار در هفته)"),
        QuizOption(3, "Almost Daily / Compulsively", "تقریباً هر روز / بدون کنترل")
    )

    val questions: List<ScientificQuizQuestion> = listOf(
        ScientificQuizQuestion(
            id = 1,
            dimensionEn = "1. LOSS OF SELF-CONTROL (BPS)",
            dimensionFa = "۱. کاهش کنترل اراده",
            questionEn = "How often do you find yourself viewing explicit content or masturbating even after promising yourself you would stop?",
            questionFa = "چند وقت یک‌بار با وجود اینکه به خودتان قول داده‌اید ترک کنید، دوباره به سراغ محتوای پورن یا خودارضایی می‌روید؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 2,
            dimensionEn = "2. DOPAMINE ESCALATION & TOLERANCE",
            dimensionFa = "۲. تحمل و نیاز به محرک بیشتر",
            questionEn = "Have you noticed needing longer sessions, more extreme content, or endless scrolling (Reels / Search) to feel the same excitement?",
            questionFa = "آیا احساس می‌کنید برای رسیدن به هیجان قبلی، به زمان بیشتر، محتوای شدیدتر یا جستجوی بی‌پایان نیاز دارید؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 3,
            dimensionEn = "3. EMOTIONAL & STRESS COPING (PPCS)",
            dimensionFa = "۳. فرار از استرس و تنهایی",
            questionEn = "Do you use porn or masturbation as an automatic escape when you feel stressed, bored, anxious, lonely, or unable to sleep?",
            questionFa = "آیا هنگام استرس، خستگی، تنهایی، بی‌حوصلگی یا بی‌خوابی، به صورت خودکار به سراغ این عادت می‌روید؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 4,
            dimensionEn = "4. LATE-NIGHT TRIGGERS & SLEEP LOSS",
            dimensionFa = "۴. بیداری شبانه و اختلال خواب",
            questionEn = "How often does late-night phone browsing lead to urges that delay your sleep or leave you exhausted the next morning?",
            questionFa = "چند وقت یک‌بار وب‌گردی شبانه باعث بیداری طولانی، وسوسه و خستگی شدید در صبح روز بعد می‌شود؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 5,
            dimensionEn = "5. BRAIN FOG & FOCUS DEPLETION",
            dimensionFa = "۵. افت تمرکز و مه‌آلودگی ذهن",
            questionEn = "After relapsing, do you experience brain fog, low motivation, social anxiety, or difficulty concentrating on study/work?",
            questionFa = "آیا پس از لغزش، دچار افت شدید انگیزه، کاهش تمرکز در کار یا درس، و احساس گناه یا بی‌حوصلگی می‌شوید؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 6,
            dimensionEn = "6. CRAVING & PREOCCUPATION",
            dimensionFa = "۶. درگیری ذهنی با محرک‌ها",
            questionEn = "When you see a slight trigger on Instagram Reels, Telegram, or the web, how hard is it to dismiss the urge immediately?",
            questionFa = "وقتی در اینستاگرام، تلگرام یا وب با یک تصویر محرک روبه‌رو می‌شوید، چقدر برایتان سخت است که فوراً از آن خارج شوید؟",
            options = listOf(
                QuizOption(0, "Very Easy — I ignore it immediately", "بسیار آسان — فوراً نادیده می‌گیرم"),
                QuizOption(1, "Somewhat Hard — Takes effort to close", "کمی دشوار — با تلاش صفحه را می‌بندم"),
                QuizOption(2, "Hard — It lingers in my mind for hours", "دشوار — ساعت‌ها ذهنم را درگیر می‌کند"),
                QuizOption(3, "Extremely Hard — Almost always leads to a slip", "بسیار دشوار — اغلب منجر به لغزش می‌شود")
            )
        ),
        ScientificQuizQuestion(
            id = 7,
            dimensionEn = "7. REAL-LIFE VITALITY & ENERGY",
            dimensionFa = "۷. تأثیر بر انرژی و روابط واقعی",
            questionEn = "Has this habit reduced your natural confidence, physical energy, or desire for real-world goals and healthy relationships?",
            questionFa = "آیا این عادت باعث کاهش اعتمادبه‌نفس، انرژی بدنی یا اشتیاق شما برای اهداف واقعی زندگی شده است؟",
            options = standardFrequencyOptions
        ),
        ScientificQuizQuestion(
            id = 8,
            dimensionEn = "8. PAST ATTEMPTS TO QUIT",
            dimensionFa = "۸. تلاش‌های قبلی برای ترک",
            questionEn = "How many times have you tried to build a clean streak on your own without an active blocker shield?",
            questionFa = "چند بار پیش از این تلاش کرده‌اید به تنهایی و بدون مسدودکننده هوشمند، یک زنجیره پاکی طولانی بسازید؟",
            options = listOf(
                QuizOption(0, "First time exploring self-mastery", "اولین بار است که تصمیم گرفته‌ام"),
                QuizOption(1, "1–2 times before", "۱ تا ۲ بار در گذشته"),
                QuizOption(2, "3–5 times, usually slipping after a few days", "۳ تا ۵ بار (معمولاً بعد از چند روز شکسته شده)"),
                QuizOption(3, "Many times — I need a strict shield now", "دفعات زیاد — اکنون به یک سپر محافظتی جدی نیاز دارم")
            )
        )
    )

    data class QuizEvaluation(
        val scorePercent: Int,
        val severityEn: String,
        val severityFa: String,
        val analysisEn: String,
        val analysisFa: String,
        val recommendationEn: String,
        val recommendationFa: String,
        val badgeColor: Color
    )

    fun evaluateScore(totalPoints: Int): QuizEvaluation {
        val maxPoints = questions.size * 3 // 24
        val pct = ((totalPoints.toFloat() / maxPoints.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        return when {
            pct >= 68 -> QuizEvaluation(
                scorePercent = pct,
                severityEn = "High Dopamine Dependency",
                severityFa = "وابستگی شدید دوپامینی (نیازمند بازسازی فوری)",
                analysisEn = "Clinical indicators show a strong compulsive dopamine loop triggered by visual cues, stress, and late-night browsing.",
                analysisFa = "نتایج علمی نشان‌دهنده چرخه قوی وابستگی دوپامینی در برابر محرک‌های تصویری، استرس و وب‌گردی شبانه است.",
                recommendationEn = "Recommended Protocol: Turn ON X Blocker Master Shield, Instagram Reels & Telegram Search Blockers, and Prevent Uninstall for a full 90-day neuro-reset.",
                recommendationFa = "پروتکل پیشنهادی: سپر اصلی ایکس بلاکر، مسدودساز ریلز و جستجوی تلگرام و محافظ ضدحذف را برای دوره کامل ۹۰ روزه روشن کنید.",
                badgeColor = CrimsonPrimary
            )
            pct >= 40 -> QuizEvaluation(
                scorePercent = pct,
                severityEn = "Moderate Compulsive Habit",
                severityFa = "عادت و وابستگی متوسط (در معرض خطر)",
                analysisEn = "Your brain has built conditioned pathways around boredom and social media triggers that periodically break your clean chain.",
                analysisFa = "مغز شما در برابر بی‌حوصلگی و محرک‌های شبکه‌های اجتماعی شرطی شده و هر از گاهی زنجیره پاکی شما را می‌شکند.",
                recommendationEn = "Recommended Protocol: Commit to a 30-to-90 day unbroken chain, use Panic SOS breathing when urges hit, and watch your Best Chain grow each clean day.",
                recommendationFa = "پروتکل پیشنهادی: متعهد به زنجیره ۳۰ تا ۹۰ روزه شوید، هنگام وسوسه از دکمه اضطراری PANIC SOS استفاده کنید و هر روز پاک شاهد رشد بهترین زنجیره خود باشید.",
                badgeColor = AmberWarning
            )
            pct >= 18 -> QuizEvaluation(
                scorePercent = pct,
                severityEn = "Mild Habit Loop",
                severityFa = "وابستگی خفیف (قابل مهار سریع)",
                analysisEn = "Early-stage habit loop detected. Stopping triggers now prevents deeper dopamine desensitization.",
                analysisFa = "نشانه‌های اولیه عادت دیده می‌شود. مهار محرک‌ها از همین امروز مانع از وابستگی عمیق‌تر خواهد شد.",
                recommendationEn = "Recommended Protocol: Maintain your daily Clean Streak Calendar and block impulsive search & short-video traps.",
                recommendationFa = "پروتکل پیشنهادی: تقویم روزهای پاک خود را حفظ کنید و تله‌های ویدیوهای کوتاه و جستجو را مسدود نگه دارید.",
                badgeColor = CyanTelemetry
            )
            else -> QuizEvaluation(
                scorePercent = pct,
                severityEn = "Healthy Self-Control",
                severityFa = "کنترل سالم و تسلط بالا بر نفس",
                analysisEn = "Low dependency score! You have strong baseline self-regulation and are using X Blocker proactively for peak focus.",
                analysisFa = "امتیاز وابستگی شما پایین است! شما کنترل خوبی دارید و از ایکس بلاکر برای تمرکز حداکثری استفاده می‌کنید.",
                recommendationEn = "Recommended Protocol: Use X Blocker's daily streak chain and app time limits to reach peak mental clarity.",
                recommendationFa = "پروتکل پیشنهادی: از زنجیره روزانه و محدودیت زمان برنامه‌ها برای رسیدن به اوج تمرکز استفاده کنید.",
                badgeColor = EmeraldShield
            )
        }
    }
}

@Composable
fun ScientificAddictionQuizDialog(
    isFirstLaunchOnboarding: Boolean,
    onQuizCompleted: (scorePercent: Int, severityEn: String, severityFa: String) -> Unit,
    onDismiss: () -> Unit
) {
    val questions = ScientificAddictionQuizRepository.questions
    // Show dedicated First-Time Welcome Onboarding Section before starting the Quiz
    var showWelcomeSection by remember { mutableStateOf(isFirstLaunchOnboarding) }
    var currentStep by remember { mutableIntStateOf(0) }
    val selectedScores = remember {
        mutableStateListOf<Int>().apply {
            repeat(questions.size) { add(-1) }
        }
    }
    var showResults by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isFirstLaunchOnboarding || showResults) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isFirstLaunchOnboarding || showResults,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .testTag("scientific_addiction_quiz_dialog"),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (showWelcomeSection) {
                    // FIRST-TIME ONBOARDING WELCOME & HOW-IT-WORKS SECTION
                    FirstTimeOnboardingWelcomeSection(
                        onStartAssessment = {
                            showWelcomeSection = false
                        },
                        onSkipToApp = {
                            onQuizCompleted(0, "Healthy Self-Control", "کنترل سالم و تسلط بالا بر نفس")
                            onDismiss()
                        }
                    )
                } else {
                    // Header Row
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IndigoAccent.copy(alpha = 0.2f))
                                    .border(1.dp, IndigoAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = CyanTelemetry,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tr("Scientific Dependency Quiz", "آزمون علمی سنجش وابستگی"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ObsidianTextPrimary
                                )
                                Text(
                                    text = tr(
                                        "Clinical BPS / PPCS Assessment (NoBeep Style)",
                                        "بر اساس مقیاس علمی BPS/PPCS (سبک نوبیپ)"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanTelemetry
                                )
                            }
                        }

                        if (!isFirstLaunchOnboarding || showResults) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = ObsidianTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showResults) {
                        val progress by animateFloatAsState(
                            targetValue = (currentStep + 1).toFloat() / questions.size.toFloat(),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "quiz_progress_anim"
                        )

                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width / 3 } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(180)))
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(tween(180)))
                                }
                            },
                            label = "quiz_question_transition"
                        ) { stepIdx ->
                            val question = questions[stepIdx]
                            Column {
                                // Step Counter & Progress Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(question.dimensionEn, question.dimensionFa),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = AmberWarning
                                    )
                                    Text(
                                        text = "${stepIdx + 1} / ${questions.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = CyanTelemetry,
                                    trackColor = ObsidianBorder
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Question Card
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianCard,
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = tr(question.questionEn, question.questionFa),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianTextPrimary,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Answer Options
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    question.options.forEachIndexed { idx, option ->
                                        val isSelected = selectedScores[stepIdx] == option.score
                                        val optionScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.02f else 1f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                            label = "option_scale"
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(optionScale)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    selectedScores[stepIdx] = option.score
                                                    if (stepIdx < questions.size - 1) {
                                                        currentStep = stepIdx + 1
                                                    } else {
                                                        val totalPts = selectedScores.sumOf { it.coerceAtLeast(0) }
                                                        val eval = ScientificAddictionQuizRepository.evaluateScore(totalPts)
                                                        onQuizCompleted(eval.scorePercent, eval.severityEn, eval.severityFa)
                                                        showResults = true
                                                    }
                                                }
                                                .testTag("quiz_option_$idx"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) {
                                                IndigoAccent.copy(alpha = 0.22f)
                                            } else {
                                                ObsidianCard
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) CyanTelemetry else ObsidianBorder
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = tr(option.labelEn, option.labelFa),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) CyanTelemetry else ObsidianTextPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) CyanTelemetry else ObsidianSurface
                                                        )
                                                        .border(1.dp, ObsidianBorder, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Previous / Back to Welcome Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep-- },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = tr("Previous", "سوال قبلی"),
                                        color = ObsidianTextSecondary
                                    )
                                }
                            } else if (isFirstLaunchOnboarding) {
                                OutlinedButton(
                                    onClick = { showWelcomeSection = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = tr("Back", "بازگشت"),
                                        color = ObsidianTextSecondary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = tr(
                                    "100% Private On-Device Evaluation",
                                    "کاملاً خصوصی و بدون ارسال اطلاعات"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = ObsidianTextSecondary
                            )
                        }
                    } else {
                        // Scientific Result Summary View
                        val totalPoints = selectedScores.sumOf { it.coerceAtLeast(0) }
                        val evaluation = ScientificAddictionQuizRepository.evaluateScore(totalPoints)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            evaluation.badgeColor.copy(alpha = 0.22f),
                                            IndigoAccent.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .border(1.5.dp, evaluation.badgeColor, RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = evaluation.badgeColor,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tr("DEPENDENCY SCORE", "امتیاز علمی وابستگی"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianTextSecondary
                                )
                                Text(
                                    text = "${evaluation.scorePercent}%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    color = evaluation.badgeColor
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = evaluation.badgeColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, evaluation.badgeColor)
                                ) {
                                    Text(
                                        text = tr(evaluation.severityEn, evaluation.severityFa),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = evaluation.badgeColor,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clinical Analysis Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianCard,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = tr("Neuro-Behavioral Analysis", "تحلیل علمی و عصبی"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanTelemetry
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tr(evaluation.analysisEn, evaluation.analysisFa),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 90-Day Recovery Recommendation Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldShield.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EmeraldShield,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tr("Your Recovery Plan", "برنامه پیشنهادی ریبوت مغز"),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldShield
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tr(evaluation.recommendationEn, evaluation.recommendationFa),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    for (i in 0 until questions.size) {
                                        selectedScores[i] = -1
                                    }
                                    currentStep = 0
                                    showResults = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("retake_quiz_inside_dialog_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CyanTelemetry)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = CyanTelemetry,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tr("Retake", "تکرار آزمون"),
                                    color = CyanTelemetry,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("finish_quiz_continue_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldShield,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = tr("Start Clean Chain", "شروع زنجیره پاکی"),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstTimeOnboardingWelcomeSection(
    onStartAssessment: () -> Unit,
    onSkipToApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("first_time_onboarding_welcome_section"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Minimal White-Background App Icon Header
        Image(
            painter = painterResource(id = R.drawable.ic_stop_sign_x_blocker_1794123976741),
            contentDescription = "X Blocker Minimal Icon",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, EmeraldShield, RoundedCornerShape(20.dp))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tr("Welcome to X Blocker", "به ایکس بلاکر خوش آمدید"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = ObsidianTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(
                    "Minimal, Private & Smart Dopamine Recovery Shield",
                    "محافظ هوشمند، مینیمال و خصوصی برای رهایی از وابستگی و ساخت زنجیره پاکی"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 3 Onboarding Guide Feature Cards
        OnboardingFeatureItem(
            icon = Icons.Default.ToggleOff,
            accentColor = CyanTelemetry,
            title = tr("1. Everything Starts OFF by Default", "۱. همه گزینه‌ها در ابتدا خاموش هستند"),
            description = tr(
                "Upon installation, Master Shield, Strict Mode, and Section Blockers are OFF so you can enable only the protections you choose.",
                "هنگام نصب، تمام قفل‌ها و مسدودسازها خاموش هستند تا با خیال راحت فقط بخش‌های دلخواه خودتان را روشن کنید."
            )
        )

        OnboardingFeatureItem(
            icon = Icons.Default.VisibilityOff,
            accentColor = CrimsonPrimary,
            title = tr("2. Reels & Telegram Search Guard", "۲. مسدودساز ریلز اینستاگرام و جستجوی تلگرام"),
            description = tr(
                "Blocks Instagram Reels & Telegram Global Search (keeping Archived Chats safe) and allows Phone Calls & SMS on the Block Screen.",
                "فقط بخش ریلز اینستاگرام و جستجوی تلگرام را مسدود می‌کند (بدون مسدودسازی آرشیو) و در صفحه قفل امکان تماس و پیامک دارید."
            )
        )

        OnboardingFeatureItem(
            icon = Icons.Default.DateRange,
            accentColor = EmeraldShield,
            title = tr("3. Clean Streak Calendar", "۳. تقویم زنجیره پاکی"),
            description = tr(
                "Track your Duolingo-style 7-day clean calendar and grow your Best Chain every clean day.",
                "تقویم زنجیره روزهای پاک به سبک دولینگو و رشد بهترین زنجیره با هر روز پاک."
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = onStartAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("onboarding_start_quiz_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldShield,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tr(
                    "Continue to 1-Min Scientific Quiz →",
                    "شروع آزمون علمی ۱ دقیقه‌ای (نوبیپ) ←"
                ),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelLarge
            )
        }

        TextButton(
            onClick = onSkipToApp,
            modifier = Modifier.testTag("onboarding_skip_to_app_button")
        ) {
            Text(
                text = tr("Skip Quiz & Enter App Directly", "رد شدن از آزمون و ورود مستقیم به برنامه"),
                style = MaterialTheme.typography.labelMedium,
                color = ObsidianTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OnboardingFeatureItem(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
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
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = ObsidianTextSecondary
                )
            }
        }
    }
}
