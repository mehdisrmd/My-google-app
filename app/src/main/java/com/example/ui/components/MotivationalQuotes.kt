package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppStrings.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianTextPrimary
import com.example.ui.theme.ObsidianTextSecondary

data class MotivationalQuote(
    val id: Int,
    val quoteEn: String,
    val quoteFa: String,
    val authorEn: String,
    val authorFa: String,
    val categoryBadgeEn: String,
    val categoryBadgeFa: String,
    val isChainMotto: Boolean = false
)

object MotivationalQuotesRepository {
    val allQuotes: List<MotivationalQuote> = listOf(
        MotivationalQuote(
            id = 1,
            quoteEn = "Don't break the chain! Every single clean day links to the next — protect today's link with everything you have.",
            quoteFa = "زنجیره را نشکن! هر روزِ پاک به روز بعدی متصل است — با تمام وجود از حلقه امروز محافظت کن.",
            authorEn = "Jerry Seinfeld • The Chain Method",
            authorFa = "جری ساینفلد • قانون زنجیره پیوستگی",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 2,
            quoteEn = "You have power over your mind — not outside events. Realize this, and you will find unbreakable strength.",
            quoteFa = "تو بر ذهن خود پادشاهی می‌کنی، نه بر حوادث بیرونی. این را دریاب تا به قدرتی شکست‌ناپذیر برسی.",
            authorEn = "Marcus Aurelius • Meditations",
            authorFa = "مارکوس اورلیوس • تأملات",
            categoryBadgeEn = "STOIC WISDOM",
            categoryBadgeFa = "حکمت رواقی"
        ),
        MotivationalQuote(
            id = 3,
            quoteEn = "No man is free who is not master of himself. Conquer the impulse and you conquer your destiny.",
            quoteFa = "هیچ انسانی آزاد نیست مگر آنکه اربابِ خویشتن باشد. بر هوس غلبه کن تا سرنوشتت را فتح کنی.",
            authorEn = "Epictetus",
            authorFa = "اپیکتتوس (فیلسوف رواقی)",
            categoryBadgeEn = "SELF-MASTERY",
            categoryBadgeFa = "تسلط بر نفس"
        ),
        MotivationalQuote(
            id = 4,
            quoteEn = "Don't break the chain! A 5-minute dopamine trap is never worth resetting weeks of hard-earned freedom.",
            quoteFa = "زنجیره را نشکن! یک وسوسه ۵ دقیقه‌ای هرگز ارزش از دست دادن روزها تلاش و آزادی را ندارد.",
            authorEn = "X Blocker Iron Rule",
            authorFa = "قانون آهنین ایکس بلاکر",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 5,
            quoteEn = "Why do you stay in prison when the door is so wide open? Step out of the cage of habit into the light.",
            quoteFa = "چرا در زندان مانده‌ای وقتی در تا این اندازه باز است؟ از قفس عادت‌های کهنه به سوی نور قدم بگذار.",
            authorEn = "Rumi (Jalāl al-Dīn Muḥammad Rūmī)",
            authorFa = "مولانا جلال‌الدین محمد بلخی",
            categoryBadgeEn = "PERSIAN MASTERPIECE",
            categoryBadgeFa = "کلام مولانا"
        ),
        MotivationalQuote(
            id = 6,
            quoteEn = "Every action you take is a vote for the type of person you wish to become. Don't break the chain of votes.",
            quoteFa = "هر عملی که انجام می‌دهی یک رأی به شخصیتی است که می‌خواهی بسازی. زنجیره تصمیم‌های درستت را نشکن!",
            authorEn = "James Clear • Atomic Habits",
            authorFa = "جیمز کلیر • عادت‌های اتمی",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 7,
            quoteEn = "We suffer more often in imagination than in reality. The urge feels urgent, but it is just a passing cloud.",
            quoteFa = "ما بیشتر در خیالمان رنج می‌کشیم تا در واقعیت. وسوسه فوری به نظر می‌رسد، اما فقط ابری گذراست.",
            authorEn = "Seneca",
            authorFa = "سنکا",
            categoryBadgeEn = "URGE SURFING",
            categoryBadgeFa = "مدیریت وسوسه"
        ),
        MotivationalQuote(
            id = 8,
            quoteEn = "I count him braver who overcomes his desires than him who conquers his enemies; for the hardest victory is over self.",
            quoteFa = "کسی که بر خواهش‌های نفس خود غلبه کند شجاع‌تر از فاتح میدان جنگ است؛ زیرا سخت‌ترین پیروزی، پیروزی بر خویشتن است.",
            authorEn = "Aristotle",
            authorFa = "ارسطو",
            categoryBadgeEn = "FAMOUS QUOTE",
            categoryBadgeFa = "سخن بزرگان"
        ),
        MotivationalQuote(
            id = 9,
            quoteEn = "Between stimulus and response there is a space. In that space is our power to choose our response.",
            quoteFa = "میان محرک و پاسخ، فاصله‌ای هست. در آن فاصله، قدرتِ انتخاب و آزادی و رشد ما نهفته است.",
            authorEn = "Viktor E. Frankl",
            authorFa = "ویکتور فرانکل",
            categoryBadgeEn = "MINDSET",
            categoryBadgeFa = "قدرت انتخاب"
        ),
        MotivationalQuote(
            id = 10,
            quoteEn = "Patience is bitter at first, but when its fruit ripens, it is sweeter than honey. Guard your streak today.",
            quoteFa = "صبر تلخ است ولیکن برِ شیرین دارد. امروز از زنجیره روزهای پاکت محافظت کن.",
            authorEn = "Saadi Shirazi",
            authorFa = "سعدی شیرازی",
            categoryBadgeEn = "PERSIAN WISDOM",
            categoryBadgeFa = "پند سعدی"
        ),
        MotivationalQuote(
            id = 11,
            quoteEn = "Do not pray for an easy life, pray for the strength to endure a difficult one. Discipline is your armor.",
            quoteFa = "برای یک زندگی آسان دعا نکن؛ برای قدرتی تلاش کن که از پس سختی‌ها بربیاید. نظم زره توست.",
            authorEn = "Bruce Lee",
            authorFa = "بروس لی",
            categoryBadgeEn = "WARRIOR SPIRIT",
            categoryBadgeFa = "روحیه جنگجو"
        ),
        MotivationalQuote(
            id = 12,
            quoteEn = "Don't break the chain! One day at a time, one hour at a time, one urge defeated at a time.",
            quoteFa = "زنجیره را نشکن! روز به روز، ساعت به ساعت، و با شکست دادن هر وسوسه، قوی‌تر می‌شوی.",
            authorEn = "Recovery Brotherhood",
            authorFa = "قانون پیوستگی",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        )
    )

    fun getQuoteByIndex(index: Int): MotivationalQuote {
        val safeIdx = ((index % allQuotes.size) + allQuotes.size) % allQuotes.size
        return allQuotes[safeIdx]
    }
}

@Composable
fun MotivationalQuoteBannerCard(
    streakDays: Int = 0,
    initialQuoteIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(initialQuoteIndex) }
    val currentQuote = remember(currentIndex) {
        MotivationalQuotesRepository.getQuoteByIndex(currentIndex)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { currentIndex = (currentIndex + 1) % MotivationalQuotesRepository.allQuotes.size }
            .testTag("motivational_quote_banner_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.5.dp,
            if (currentQuote.isChainMotto) AmberWarning.copy(alpha = 0.65f) else IndigoAccent.copy(alpha = 0.55f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = if (currentQuote.isChainMotto) {
                            listOf(
                                AmberWarning.copy(alpha = 0.16f),
                                EmeraldShield.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                IndigoAccent.copy(alpha = 0.16f),
                                CyanTelemetry.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentQuote.isChainMotto) {
                                AmberWarning.copy(alpha = 0.2f)
                            } else {
                                IndigoAccent.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (currentQuote.isChainMotto) AmberWarning else IndigoAccent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentQuote.isChainMotto) Icons.Default.Link else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (currentQuote.isChainMotto) AmberWarning else IndigoAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tr(currentQuote.categoryBadgeEn, currentQuote.categoryBadgeFa),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = if (currentQuote.isChainMotto) AmberWarning else IndigoAccent
                                )
                            }
                        }

                        if (streakDays > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("🔗 $streakDays-Day Chain", "🔗 زنجیره $streakDays روزه"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldShield
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            currentIndex = (currentIndex + 1) % MotivationalQuotesRepository.allQuotes.size
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.testTag("next_motivational_quote_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next Quote",
                                tint = CyanTelemetry,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Next Quote", "جمله بعدی"),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanTelemetry
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = if (currentQuote.isChainMotto) AmberWarning else CyanTelemetry,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"${tr(currentQuote.quoteEn, currentQuote.quoteFa)}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            color = ObsidianTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "— ${tr(currentQuote.authorEn, currentQuote.authorFa)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTextSecondary
                        )
                    }
                }
            }
        }
    }
}
