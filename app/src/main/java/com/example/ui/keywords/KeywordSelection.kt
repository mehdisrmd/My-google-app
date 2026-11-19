package com.example.ui.keywords

/**
 * Pure selection-state math for the Custom Blacklist multi-select mode (#15).
 *
 * The screen keeps the selection as an immutable `Set<Long>` of keyword ids
 * in Compose state; every transition goes through these helpers so the logic
 * can be unit-tested without a device. All functions are side-effect free and
 * return the SAME set instance when nothing changes, so callers can assign
 * the result straight back into Compose state without causing extra
 * recompositions.
 */
object KeywordSelection {

    /** Adds [id] when absent, removes it when present. */
    fun toggle(current: Set<Long>, id: Long): Set<Long> =
        if (id in current) current - id else current + id

    /** Adds every id in [ids] (e.g. "select all" over the visible rows). */
    fun selectAll(current: Set<Long>, ids: Collection<Long>): Set<Long> =
        if (ids.isEmpty() || current.containsAll(ids)) current else current + ids

    /** Drops ids that no longer exist (rows deleted or list reloaded). */
    fun retainExisting(current: Set<Long>, existingIds: Collection<Long>): Set<Long> {
        if (current.isEmpty()) return current
        val pruned = current.intersect(existingIds.toSet())
        return if (pruned.size == current.size) current else pruned
    }

    /** True when every id in [ids] is selected; false for an empty [ids]. */
    fun isAllSelected(selected: Set<Long>, ids: Collection<Long>): Boolean =
        ids.isNotEmpty() && selected.containsAll(ids)
}
