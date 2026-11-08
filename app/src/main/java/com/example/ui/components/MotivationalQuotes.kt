package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import java.util.concurrent.TimeUnit

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
            quoteEn = "Don't break the chain! A 5-minute dopamine trap is never worth trading weeks of hard-earned freedom.",
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
        ),
        MotivationalQuote(
            id = 13,
            quoteEn = "He who conquers others is strong; He who conquers himself is mighty.",
            quoteFa = "کسی که بر دیگران چیره شود نیرومند است، اما کسی که بر خویشتن چیره شود شکست‌ناپذیر است.",
            authorEn = "Lao Tzu • Tao Te Ching",
            authorFa = "لائوتسه • دائو ده جینگ",
            categoryBadgeEn = "ANCIENT WISDOM",
            categoryBadgeFa = "حکمت کهن"
        ),
        MotivationalQuote(
            id = 14,
            quoteEn = "Discipline is choosing between what you want now and what you want most. Don't break the chain!",
            quoteFa = "انضباط یعنی انتخاب میان آنچه همین لحظه هوس کرده‌ای و آنچه بیش از هر چیز در زندگی می‌خواهی. زنجیره را نشکن!",
            authorEn = "Abraham Lincoln",
            authorFa = "آبراهام لینکلن",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 15,
            quoteEn = "The first and greatest victory is to conquer yourself; to be conquered by yourself is of all things most shameful.",
            quoteFa = "نخستین و بزرگ‌ترین پیروزی، فتحِ خویشتن است؛ و مغلوبِ نفسِ خود شدن از هر شکستی تلخ‌تر است.",
            authorEn = "Plato",
            authorFa = "افلاطون",
            categoryBadgeEn = "PHILOSOPHY",
            categoryBadgeFa = "فلسفه"
        ),
        MotivationalQuote(
            id = 16,
            quoteEn = "Even if a thousand griefs surround your heart, do not despair; the dawn of freedom is closer than you think.",
            quoteFa = "یوسف گمگشته بازآید به کنعان غم مخور — کلبه احزان شود روزی گلستان غم مخور.",
            authorEn = "Hafez Shirazi",
            authorFa = "حافظ شیرازی",
            categoryBadgeEn = "PERSIAN POETRY",
            categoryBadgeFa = "غزل حافظ"
        ),
        MotivationalQuote(
            id = 17,
            quoteEn = "He who has a why to live for can bear almost any how. Remember why you started this streak.",
            quoteFa = "کسی که «چرایی» بزرگی برای زیستن دارد، با هر «چگونگی» و سختی‌ای کنار می‌آید. به یاد بیاور چرا شروع کردی.",
            authorEn = "Friedrich Nietzsche",
            authorFa = "فریدریش نیچه",
            categoryBadgeEn = "PURPOSE",
            categoryBadgeFa = "هدفمندی"
        ),
        MotivationalQuote(
            id = 18,
            quoteEn = "You can never get a smaller mastery than the mastery of yourself. Guard your mind's gates.",
            quoteFa = "هیچ تسلطی کوچک‌تر یا بزرگ‌تر از تسلط بر خویشتن نیست. نگهبان دروازه‌های ذهنت باش.",
            authorEn = "Leonardo da Vinci",
            authorFa = "لئوناردو داوینچی",
            categoryBadgeEn = "GENIUS MIND",
            categoryBadgeFa = "ذهن خلاق"
        ),
        MotivationalQuote(
            id = 19,
            quoteEn = "Today is victory over yourself of yesterday; tomorrow is your victory over lesser men.",
            quoteFa = "امروز روز پیروزی تو بر دیروزِ خودت است؛ و فردا روز اوج گرفتن توست.",
            authorEn = "Miyamoto Musashi • The Book of Five Rings",
            authorFa = "میاموتو موساشی • کتاب پنج حلقه",
            categoryBadgeEn = "SAMURAI CODE",
            categoryBadgeFa = "آیین سامورایی"
        ),
        MotivationalQuote(
            id = 20,
            quoteEn = "Don't break the chain! When an urge hits, wait 15 minutes and breathe deeply — your brain will thank you tomorrow.",
            quoteFa = "زنجیره را نشکن! وقتی وسوسه سراغت آمد، ۱۵ دقیقه صبر کن و عمیق نفس بکش — فردا به خودت افتخار خواهی کرد.",
            authorEn = "Neuroscience Recovery Principle",
            authorFa = "اصل علمی بازسازی دوپامین",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 21,
            quoteEn = "Until you make the unconscious conscious, it will direct your life and you will call it fate.",
            quoteFa = "تا زمانی که عادت‌های ناخودآگاهت را آگاهانه مهار نکنی، آن‌ها زندگی‌ات را هدایت می‌کنند و نامش را سرنوشت می‌گذاری.",
            authorEn = "Carl Jung",
            authorFa = "کارل گوستاو یونگ",
            categoryBadgeEn = "PSYCHOLOGY",
            categoryBadgeFa = "روان‌شناسی"
        ),
        MotivationalQuote(
            id = 22,
            quoteEn = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            quoteFa = "ما همانی هستیم که تکرارش می‌کنیم؛ پس برتری و پاکی یک اتفاق نیست، بلکه یک عادت روزانه است.",
            authorEn = "Will Durant",
            authorFa = "ویل دورانت",
            categoryBadgeEn = "DAILY HABIT",
            categoryBadgeFa = "عادت روزانه"
        ),
        MotivationalQuote(
            id = 23,
            quoteEn = "It always seems impossible until it is done. Protect today and watch your life transform.",
            quoteFa = "هر کار بزرگی تا پیش از انجام شدنش ناممکن به نظر می‌رسد. از امروزت محافظت کن تا زندگی‌ات دگرگون شود.",
            authorEn = "Nelson Mandela",
            authorFa = "نلسون ماندلا",
            categoryBadgeEn = "RESILIENCE",
            categoryBadgeFa = "ایستادگی"
        ),
        MotivationalQuote(
            id = 24,
            quoteEn = "A man who conquers himself is greater than one who takes a thousand fortified cities.",
            quoteFa = "آدمی که بر نفس خود چیره شود، از فاتح هزار شهر و دژ نیرومندتر است.",
            authorEn = "Dhammapada",
            authorFa = "حکمت شرقی",
            categoryBadgeEn = "INNER VICTORY",
            categoryBadgeFa = "پیروزی درونی"
        ),
        MotivationalQuote(
            id = 25,
            quoteEn = "Don't break the chain! Every clean day rewires your dopamine receptors toward real joy, ambition, and peace.",
            quoteFa = "زنجیره را نشکن! هر روز پاک، گیرنده‌های دوپامین مغزت را به سوی شادی واقعی، تمرکز و آرامش بازسازی می‌کند.",
            authorEn = "Brain Rewiring Protocol",
            authorFa = "پروتکل بازسازی مغز",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 26,
            quoteEn = "The secret of change is to focus all of your energy not on fighting the old, but on building the new.",
            quoteFa = "راز تغییر در این است که تمام انرژی خود را نه صرفِ درگیری با گذشته، بلکه صرفِ ساختن آینده‌ای نو کنی.",
            authorEn = "Socrates",
            authorFa = "سقراط",
            categoryBadgeEn = "WISDOM",
            categoryBadgeFa = "حکمت سقراط"
        ),
        MotivationalQuote(
            id = 27,
            quoteEn = "It does not matter how slowly you go as long as you do not stop. Keep your chain unbroken.",
            quoteFa = "مهم نیست چقدر آهسته پیش می‌روی، تا زمانی که متوقف نشوی برنده تویی. زنجیره‌ات را حفظ کن.",
            authorEn = "Confucius",
            authorFa = "کنفوسیوس",
            categoryBadgeEn = "PERSEVERANCE",
            categoryBadgeFa = "پشتکار"
        ),
        MotivationalQuote(
            id = 28,
            quoteEn = "Above all, don't lie to yourself. Be honest with your triggers and close the door before the urge grows.",
            quoteFa = "بیش از هر چیز، به خودت دروغ نگو. با محرک‌هایت روراست باش و پیش از اوج گرفتن وسوسه، در را ببند.",
            authorEn = "Fyodor Dostoevsky • The Brothers Karamazov",
            authorFa = "فئودور داستایوفسکی • برادران کارامازوف",
            categoryBadgeEn = "LITERARY WISDOM",
            categoryBadgeFa = "سخن بزرگان"
        ),
        MotivationalQuote(
            id = 29,
            quoteEn = "He who cannot obey himself will be commanded. Choose self-command every single morning.",
            quoteFa = "کسی که نتواند به خویشتن فرمان دهد، فرمان‌بردار هوس‌ها خواهد شد. هر صبح فرمانروای خودت باش.",
            authorEn = "Friedrich Nietzsche",
            authorFa = "فریدریش نیچه",
            categoryBadgeEn = "SELF-COMMAND",
            categoryBadgeFa = "فرمانروایی بر نفس"
        ),
        MotivationalQuote(
            id = 30,
            quoteEn = "Don't break the chain! Respect the version of yourself who fought through yesterday to give you today's streak.",
            quoteFa = "زنجیره را نشکن! به تلاشِ دیروزِ خودت احترام بگذار که جنگید تا امروز این زنجیره پاک را داشته باشی.",
            authorEn = "X Blocker Daily Creed",
            authorFa = "منشور روزانه ایکس بلاکر",
            categoryBadgeEn = "DON'T BREAK THE CHAIN",
            categoryBadgeFa = "زنجیره را نشکن",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 31,
            quoteEn = "Chains of habit are too light to be felt until they are too heavy to be broken — forge a chain of freedom instead.",
            quoteFa = "زنجیرهای عادت ابتدا آن‌قدر سبک‌اند که حس نمی‌شوند تا روزی که شکستنشان دشوار شود؛ زنجیره آزادی را بساز!",
            authorEn = "Warren Buffett",
            authorFa = "وارن بافت",
            categoryBadgeEn = "HABIT CHAIN",
            categoryBadgeFa = "زنجیره عادت",
            isChainMotto = true
        ),
        MotivationalQuote(
            id = 32,
            quoteEn = "How long are you going to wait before you demand the best for yourself?",
            quoteFa = "تا کی می‌خواهی صبر کنی تا بهترین نسخه زندگی را از خودت مطالبه کنی؟ همین امروز روز توست.",
            authorEn = "Epictetus • Enchiridion",
            authorFa = "اپیکتتوس",
            categoryBadgeEn = "STOIC CALL",
            categoryBadgeFa = "ندای بیداری"
        )
    )

    fun getQuoteByIndex(index: Int): MotivationalQuote {
        val safeIdx = ((index % allQuotes.size) + allQuotes.size) % allQuotes.size
        return allQuotes[safeIdx]
    }

    /**
     * Deterministically returns one quote per calendar day so the user sees a fresh quote
     * each day without tapping.
     */
    fun getDailyQuoteForToday(nowMs: Long = System.currentTimeMillis()): MotivationalQuote {
        val epochDay = TimeUnit.MILLISECONDS.toDays(nowMs).toInt()
        return getQuoteByIndex(epochDay)
    }
}

/**
 * Non-tappable Daily Motivational Quote Card (Item 10):
 * Automatically displays one new quote each calendar day and cannot be tapped to cycle.
 */
@Composable
fun MotivationalQuoteBannerCard(
    streakDays: Int = 0,
    initialQuoteIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val currentQuote = remember(initialQuoteIndex) {
        if (initialQuoteIndex >= 0) {
            MotivationalQuotesRepository.getQuoteByIndex(initialQuoteIndex)
        } else {
            MotivationalQuotesRepository.getDailyQuoteForToday()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
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
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CyanTelemetry,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Daily Quote", "جمله روز"),
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = if (currentQuote.isChainMotto) AmberWarning else CyanTelemetry,
                            modifier = Modifier.size(16.dp)
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
