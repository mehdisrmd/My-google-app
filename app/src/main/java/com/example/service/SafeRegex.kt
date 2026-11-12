package com.example.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Compiles and runs user-supplied regexes safely.
 *
 * Keyword rules may be regexes typed by the user, and they are matched against
 * the whole screen text on the main thread. A pattern such as `(a+)+$` can
 * backtrack for minutes on a long input and freeze the app, and a try/catch
 * around a syntax error does not help with that. [containsMatch] therefore
 * runs the match under a small time budget and gives up with `null` when the
 * budget is exceeded. Compiled patterns are cached, because the scanner used
 * to rebuild every Regex on every accessibility event.
 *
 * Kept free of Android classes so it can be unit-tested on the JVM.
 */
object SafeRegex {

    /** Per-match time budget. Real keyword patterns finish in well under 5 ms. */
    const val DEFAULT_BUDGET_MS: Long = 25L

    private const val MAX_CACHE_SIZE = 500

    private val cache = ConcurrentHashMap<String, Regex>()

    /**
     * Returns the compiled [Regex] for [pattern], or `null` if the pattern is
     * not valid. Valid patterns are cached; invalid ones are not, so fixing a
     * keyword takes effect immediately.
     */
    fun compile(pattern: String, ignoreCase: Boolean = true): Regex? {

        val key = (if (ignoreCase) "i:" else "c:") + pattern
        cache[key]?.let { return it }
        val compiled = try {
            if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
        } catch (e: IllegalArgumentException) {
            // PatternSyntaxException is an IllegalArgumentException.
            return null
        }
        if (cache.size >= MAX_CACHE_SIZE) cache.clear()
        cache[key] = compiled
        return compiled
    }

    /**
     * True/false when the match finished inside [budgetMs]; `null` when it ran
     * out of time. Callers should treat `null` as "no match".
     */
    fun containsMatch(
        regex: Regex,
        text: CharSequence,
        budgetMs: Long = DEFAULT_BUDGET_MS
    ): Boolean? {
        val deadlineNanos = System.nanoTime() + budgetMs * 1_000_000L
        return try {
            regex.containsMatchIn(DeadlineCharSequence(text, deadlineNanos))
        } catch (_: RegexBudgetExceeded) {
            null
        }
    }

    /** Thrown from inside the matcher to abort a runaway match. */
    private class RegexBudgetExceeded : RuntimeException() {

        // The exception is control flow, so skip the costly stack trace.
        override fun fillInStackTrace(): Throwable = this
    }

    /**
     * Wraps the input so the regex engine's character reads double as a clock
     * check. A runaway pattern reads characters millions of times, so the
     * deadline is noticed quickly. The clock is read only every 1024 reads to
     * keep the overhead of normal matches negligible.
     */
    private class DeadlineCharSequence(
        private val inner: CharSequence,
        private val deadlineNanos: Long
    ) : CharSequence {

        private var reads = 0

        override val length: Int get() = inner.length

        override fun get(index: Int): Char {
            if ((++reads and 1023) == 0 && System.nanoTime() - deadlineNanos > 0L) {
                throw RegexBudgetExceeded()
            }
            return inner[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(inner.subSequence(startIndex, endIndex), deadlineNanos)

        override fun toString(): String = inner.toString()
    }
}
