package com.example.ui.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #25: the restricted-settings guide is pure content + one decision —
 * it is offered only while the accessibility service is off (the only
 * moment Android 13+'s sideload gate can be the blocker).
 */
class RestrictedSettingsGuideTest {

    @Test
    fun `guide is offered only while accessibility is off`() {
        assertTrue(RestrictedSettingsGuideContent.shouldOfferGuide(isAccessibilityEnabled = false))
        assertFalse(RestrictedSettingsGuideContent.shouldOfferGuide(isAccessibilityEnabled = true))
    }

    @Test
    fun `both languages have the same number of non-blank steps`() {
        val en = RestrictedSettingsGuideContent.stepsEn
        val fa = RestrictedSettingsGuideContent.stepsFa
        assertTrue(en.isNotEmpty())
        assertEquals(en.size, fa.size)
        assertTrue(en.all { it.isNotBlank() })
        assertTrue(fa.all { it.isNotBlank() })
    }

    @Test
    fun `steps selector follows the language flag`() {
        assertEquals(
            RestrictedSettingsGuideContent.stepsEn,
            RestrictedSettingsGuideContent.steps(isFa = false)
        )
        assertEquals(
            RestrictedSettingsGuideContent.stepsFa,
            RestrictedSettingsGuideContent.steps(isFa = true)
        )
    }

    @Test
    fun `english steps name the exact system actions the user must take`() {
        val joined = RestrictedSettingsGuideContent.stepsEn.joinToString(separator = " ")
        // The App-info overflow action, by its exact system label.
        assertTrue(joined.contains("Allow restricted settings"))
        // And the destination the user must reach afterwards.
        assertTrue(joined.contains("Accessibility"))
        assertTrue(joined.contains("X Blocker"))
    }
}
