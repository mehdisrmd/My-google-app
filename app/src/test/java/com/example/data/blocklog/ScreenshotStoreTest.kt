package com.example.data.blocklog

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * #14 storage contract: screenshots live ONLY in the app-private
 * `filesDir/blocklog/` folder (never MediaStore, never shared storage),
 * are downscaled on the way in, decode back for display, and are removed
 * by delete/deleteAll.
 */
@RunWith(RobolectricTestRunner::class)
class ScreenshotStoreTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private fun solidBitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    @Test
    fun `screenshots are written to the private blocklog folder only`() {
        val store = ScreenshotStore(context)
        val name = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 1L)

        assertTrue(store.saveJpeg(solidBitmap(400, 800), name))

        val stored = File(context.filesDir, "${ScreenshotStore.DIRECTORY_NAME}/$name")
        assertTrue(stored.isFile)
        assertTrue(stored.length() > 0L)
        assertTrue(stored.absolutePath.startsWith(context.filesDir.absolutePath))
        assertTrue(store.exists(name))
    }

    @Test
    fun `stored screenshots are downscaled to the policy bound`() {
        val store = ScreenshotStore(context)
        val name = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 2L)

        assertTrue(store.saveJpeg(solidBitmap(2000, 1000), name))

        val loaded = store.loadBitmap(name, BlockLogPolicy.MAX_SCREENSHOT_DIMENSION_PX)
        assertNotNull(loaded)
        assertEquals(BlockLogPolicy.MAX_SCREENSHOT_DIMENSION_PX, loaded!!.width)
        assertEquals(540, loaded.height)
    }

    @Test
    fun `loadBitmap tolerates missing and blank names`() {
        val store = ScreenshotStore(context)
        assertNull(store.loadBitmap("does-not-exist.jpg", 256))
        assertNull(store.loadBitmap(null, 256))
        assertNull(store.loadBitmap("", 256))
        assertFalse(store.exists("does-not-exist.jpg"))
    }

    @Test
    fun `delete removes exactly the named file`() {
        val store = ScreenshotStore(context)
        val keep = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 3L)
        val drop = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 4L)
        assertTrue(store.saveJpeg(solidBitmap(100, 100), keep))
        assertTrue(store.saveJpeg(solidBitmap(100, 100), drop))

        assertTrue(store.delete(drop))

        assertFalse(store.exists(drop))
        assertTrue(store.exists(keep))
        // Deleting again / deleting nothing is a no-op success.
        assertTrue(store.delete(drop))
        assertFalse(store.delete(null))
    }

    @Test
    fun `deleteAll empties the folder`() {
        val store = ScreenshotStore(context)
        val first = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 5L)
        val second = BlockLogPolicy.screenshotFileName(1_700_000_000_000L, 6L)
        assertTrue(store.saveJpeg(solidBitmap(100, 100), first))
        assertTrue(store.saveJpeg(solidBitmap(100, 100), second))

        store.deleteAll()

        assertFalse(store.exists(first))
        assertFalse(store.exists(second))
    }
}
