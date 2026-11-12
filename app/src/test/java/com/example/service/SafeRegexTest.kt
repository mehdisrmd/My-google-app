package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SafeRegexTest {

    /** Exponential-backtracking input for `(a+)+$`: many 'a's, then a character that breaks the match. */
    private val catastrophicText = "a".repeat(5000) + "b"
    private val catastrophicPattern = "(a+)+$"

    private fun compiled(pattern: String, ignoreCase: Boolean = true): Regex {
        val regex = SafeRegex.compile(pattern, ignoreCase)
        assertNotNull("expected '$pattern' to compile", regex)
        return regex!!
    }

    // ---- compile ----

    @Test
    fun compile_validPattern_returnsARegex() {
        assertNotNull(SafeRegex.compile("abc"))
    }


    @Test
    fun compile_isCaseInsensitiveByDefault() {
        assertEquals(true, SafeRegex.containsMatch(compiled("abc"), "xxABCxx"))
    }

    @Test
    fun compile_withIgnoreCaseFalse_isCaseSensitive() {
        val regex = compiled("abc", ignoreCase = false)
        assertEquals(false, SafeRegex.containsMatch(regex, "xxABCxx"))
        assertEquals(true, SafeRegex.containsMatch(regex, "xxabcxx"))
    }

    @Test
    fun compile_invalidPattern_returnsNull_insteadOfThrowing() {
        assertNull(SafeRegex.compile("("))
        assertNull(SafeRegex.compile("[a-"))
        assertNull(SafeRegex.compile("*abc"))
    }

    @Test
    fun compile_invalidPattern_isNotCached_soAFixedKeywordWorksImmediately() {
        assertNull(SafeRegex.compile("(abc"))
        assertNull(SafeRegex.compile("(abc"))
        // The user corrects the keyword: the corrected pattern compiles at once.
        assertNotNull(SafeRegex.compile("(abc)"))
    }

    @Test
    fun compile_samePatternTwice_returnsTheCachedInstance() {
        val first = SafeRegex.compile("cache-me-please")
        val second = SafeRegex.compile("cache-me-please")

        assertNotNull(first)
        assertSame(first, second)
    }

    @Test
    fun compile_cacheSeparatesTheIgnoreCaseFlag() {
        val insensitive = compiled("Flag", ignoreCase = true)
        val sensitive = compiled("Flag", ignoreCase = false)
        assertNotSame(insensitive, sensitive)
        assertEquals(true, SafeRegex.containsMatch(insensitive, "flag"))
        assertEquals(false, SafeRegex.containsMatch(sensitive, "flag"))
    }

    @Test
    fun compile_persianPattern_matchesPersianText() {
        val regex = compiled("آرشیو")
        assertEquals(true, SafeRegex.containsMatch(regex, "چت‌های آرشیو شده"))
        assertEquals(false, SafeRegex.containsMatch(regex, "چت‌های عادی"))
    }

    @Test
    fun compile_staysCorrectAfterTheCacheHasBeenFilledPastItsLimit() {
        // The cache is capped and cleared when full; behavior must not change.
        for (i in 0 until 650) {
            assertNotNull(SafeRegex.compile("fill-the-cache-$i"))
        }
        val regex = compiled("fill-the-cache-3")
        assertEquals(true, SafeRegex.containsMatch(regex, "xx fill-the-cache-3 xx"))
        assertEquals(false, SafeRegex.containsMatch(regex, "xx fill-the-cache-4 xx"))
    }

    // ---- containsMatch: normal matching ----


    @Test
    fun containsMatch_findsAMatch() {
        assertEquals(true, SafeRegex.containsMatch(compiled("needle"), "hay needle hay"))
    }

    @Test
    fun containsMatch_noMatch_isFalseNotNull() {
        assertEquals(false, SafeRegex.containsMatch(compiled("needle"), "just hay"))
    }

    @Test
    fun containsMatch_emptyText_isFalse() {
        assertEquals(false, SafeRegex.containsMatch(compiled("needle"), ""))
    }

    @Test
    fun containsMatch_anchorsBehaveLikeNormalRegex() {
        val regex = compiled("^abc$")
        assertEquals(true, SafeRegex.containsMatch(regex, "abc"))
        assertEquals(false, SafeRegex.containsMatch(regex, "xabc"))
        assertEquals(false, SafeRegex.containsMatch(regex, "abcx"))
    }

    @Test
    fun containsMatch_sameResultAsPlainRegex_forOrdinaryPatterns() {
        val cases = listOf(
            "colou?r" to "what a colour",
            "\\d{3}-\\d{4}" to "call 555-1234 now",
            "(cat|dog)s?" to "two dogs",
            "[a-z]+@[a-z]+\\.com" to "mail me at bob@example.com",
            "^no match here$" to "something else"

        )
        for ((pattern, text) in cases) {
            val regex = compiled(pattern)
            assertEquals("pattern=$pattern", regex.containsMatchIn(text), SafeRegex.containsMatch(regex, text))
        }
    }

    @Test
    fun containsMatch_longBenignInput_finishesWithTheRightAnswer() {
        val text = "a".repeat(200_000) + "needle"
        // Generous budget: this checks correctness on a long input, not speed.
        assertEquals(true, SafeRegex.containsMatch(compiled("needle"), text, budgetMs = 10_000L))
        assertEquals(false, SafeRegex.containsMatch(compiled("zzz"), text, budgetMs = 10_000L))
    }

    // ---- containsMatch: the time budget ----

    @Test
    fun containsMatch_runawayPattern_givesUpAndReturnsNull() {
        val regex = compiled(catastrophicPattern, ignoreCase = false)
        val startNanos = System.nanoTime()
        val result = SafeRegex.containsMatch(regex, catastrophicText, budgetMs = 50L)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L

        assertNull("a runaway match must report 'out of time', not a result", result)
        // Left alone this input takes minutes; the budget cuts it off. The bound
        // is generous so a slow machine does not make the test flaky.
        assertTrue("took ${elapsedMs}ms, the 50ms budget was not enforced", elapsedMs < 5_000L)
    }

    @Test
    fun containsMatch_alreadyExpiredBudget_returnsNullOnALongInput() {

        val regex = compiled("zzz")
        val text = "a".repeat(100_000)
        assertNull(SafeRegex.containsMatch(regex, text, budgetMs = -1_000L))
    }

    @Test
    fun containsMatch_afterATimeout_theNextMatchStillWorks() {
        val runaway = compiled(catastrophicPattern, ignoreCase = false)
        assertNull(SafeRegex.containsMatch(runaway, catastrophicText, budgetMs = 30L))

        // The timeout must not leave any state behind that spoils later matches.
        assertEquals(true, SafeRegex.containsMatch(compiled("needle"), "hay needle hay"))
        assertEquals(false, SafeRegex.containsMatch(compiled("needle"), "hay"))
    }

    @Test
    fun containsMatch_runawayThenNormal_inALoop_neverLeaksATimeout() {
        val runaway = compiled(catastrophicPattern, ignoreCase = false)
        val normal = compiled("ok")
        repeat(3) {
            assertNull(SafeRegex.containsMatch(runaway, catastrophicText, budgetMs = 20L))
            assertEquals(true, SafeRegex.containsMatch(normal, "all ok"))
        }
    }

    @Test
    fun defaultBudget_isASmallPositiveNumberOfMilliseconds() {
        assertTrue(SafeRegex.DEFAULT_BUDGET_MS > 0L)
        assertTrue(SafeRegex.DEFAULT_BUDGET_MS <= 100L)
    }

    // ---- several threads at once (service thread + ticker thread) ----


    @Test
    fun compileAndMatch_fromManyThreads_areConsistent() {
        val threads = 8
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val failures = AtomicInteger(0)
        val firstError = AtomicReference<Throwable?>(null)

        val workers = (0 until threads).map { index ->
            Thread {
                try {
                    ready.countDown()
                    go.await()
                    for (i in 0 until 200) {
                        val pattern = "thread-pattern-${(i + index) % 40}"
                        val regex = SafeRegex.compile(pattern)
                        if (regex == null || SafeRegex.containsMatch(regex, "x $pattern y") != true) {
                            failures.incrementAndGet()
                        }
                    }
                } catch (t: Throwable) {
                    failures.incrementAndGet()
                    firstError.compareAndSet(null, t)
                }
            }.also { it.start() }
        }

        ready.await()
        go.countDown()
        workers.forEach { it.join(30_000L) }


        assertEquals("first error: ${firstError.get()}", 0, failures.get())
    }
}
