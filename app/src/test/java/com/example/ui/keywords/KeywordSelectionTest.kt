package com.example.ui.keywords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Selection-state math behind the Custom Blacklist multi-select mode (#15):
 * long-press selects rows, the toolbar deletes every selected id at once.
 * All functions under test are pure set transitions.
 */
class KeywordSelectionTest {

    // ---- toggle ----

    @Test
    fun toggleAddsAnUnselectedId() {
        assertEquals(setOf(7L), KeywordSelection.toggle(emptySet(), 7L))
        assertEquals(setOf(1L, 7L), KeywordSelection.toggle(setOf(1L), 7L))
    }

    @Test
    fun toggleRemovesASelectedId() {
        assertEquals(setOf(1L), KeywordSelection.toggle(setOf(1L, 7L), 7L))
        assertEquals(emptySet<Long>(), KeywordSelection.toggle(setOf(7L), 7L))
    }

    @Test
    fun toggleTwiceReturnsToTheOriginalSelection() {
        val start = setOf(1L, 2L)
        val once = KeywordSelection.toggle(start, 3L)
        assertEquals(start, KeywordSelection.toggle(once, 3L))
    }

    // ---- selectAll ----

    @Test
    fun selectAllAddsEveryVisibleId() {
        val result = KeywordSelection.selectAll(setOf(1L), listOf(2L, 3L, 4L))
        assertEquals(setOf(1L, 2L, 3L, 4L), result)
    }

    @Test
    fun selectAllKeepsSelectionsOutsideTheVisibleIds() {
        // Rows hidden by the search filter stay selected.
        val result = KeywordSelection.selectAll(setOf(99L), listOf(1L, 2L))
        assertEquals(setOf(99L, 1L, 2L), result)
    }

    @Test
    fun selectAllWithNoIdsIsANoOp() {
        val start = setOf(1L)
        assertSame(start, KeywordSelection.selectAll(start, emptyList()))
    }

    // ---- retainExisting ----

    @Test
    fun retainExistingDropsDeletedIds() {
        val result = KeywordSelection.retainExisting(setOf(1L, 2L, 3L), listOf(1L, 3L, 4L))
        assertEquals(setOf(1L, 3L), result)
    }

    @Test
    fun retainExistingKeepsEverythingWhenAllRowsStillExist() {
        val start = setOf(1L, 2L)
        assertSame(start, KeywordSelection.retainExisting(start, listOf(1L, 2L, 3L)))
    }

    @Test
    fun retainExistingOfEmptySelectionStaysEmpty() {
        assertEquals(emptySet<Long>(), KeywordSelection.retainExisting(emptySet(), listOf(1L)))
    }

    // ---- isAllSelected ----

    @Test
    fun isAllSelectedOnlyWhenEveryVisibleIdIsSelected() {
        assertTrue(KeywordSelection.isAllSelected(setOf(1L, 2L, 3L), listOf(1L, 2L)))
        assertFalse(KeywordSelection.isAllSelected(setOf(1L), listOf(1L, 2L)))
        assertFalse(KeywordSelection.isAllSelected(emptySet(), listOf(1L)))
    }

    @Test
    fun isAllSelectedIsFalseForAnEmptyList() {
        assertFalse(KeywordSelection.isAllSelected(setOf(1L), emptyList()))
        assertFalse(KeywordSelection.isAllSelected(emptySet(), emptyList()))
    }

    // ---- delete flow ----

    @Test
    fun selectionIsEmptyAfterDeletingTheSelectedRows() {
        // Select three rows, delete them, then prune against the survivors:
        // nothing may remain selected.
        var selected = KeywordSelection.selectAll(emptySet(), listOf(1L, 2L, 3L))
        selected = KeywordSelection.retainExisting(selected, listOf(4L, 5L))
        assertEquals(emptySet<Long>(), selected)
    }
}
