package manoellribeiro.dev.martp.core.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ViewExtensionsTest {

    private lateinit var view: View
    private lateinit var bitmap: Bitmap

    @BeforeEach
    fun setup() {
        view = mockk()
        bitmap = mockk()
        mockkStatic(Bitmap::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // gone

    @Test
    fun `if view is visible should set its visibility to gone`() {
        // Given
        every { view.visibility } returns View.VISIBLE
        every { view.visibility = View.GONE } just runs

        // When
        view.gone()

        // Then
        verify(exactly = 1) { view.visibility = View.GONE }
    }

    @Test
    fun `if view is invisible should set its visibility to gone`() {
        // Given
        every { view.visibility } returns View.INVISIBLE
        every { view.visibility = View.GONE } just runs

        // When
        view.gone()

        // Then
        verify(exactly = 1) { view.visibility = View.GONE }
    }

    @Test
    fun `if view is already gone should not set its visibility again on gone`() {
        // Given
        every { view.visibility } returns View.GONE

        // When
        view.gone()

        // Then
        verify(exactly = 0) { view.visibility = any() }
    }

    // visible

    @Test
    fun `if view is gone should set its visibility to visible`() {
        // Given
        every { view.visibility } returns View.GONE
        every { view.visibility = View.VISIBLE } just runs

        // When
        view.visible()

        // Then
        verify(exactly = 1) { view.visibility = View.VISIBLE }
    }

    @Test
    fun `if view is invisible should set its visibility to visible`() {
        // Given
        every { view.visibility } returns View.INVISIBLE
        every { view.visibility = View.VISIBLE } just runs

        // When
        view.visible()

        // Then
        verify(exactly = 1) { view.visibility = View.VISIBLE }
    }

    @Test
    fun `if view is already visible should not set its visibility again on visible`() {
        // Given
        every { view.visibility } returns View.VISIBLE

        // When
        view.visible()

        // Then
        verify(exactly = 0) { view.visibility = any() }
    }

    // invisible

    @Test
    fun `if view is visible should set its visibility to invisible`() {
        // Given
        every { view.visibility } returns View.VISIBLE
        every { view.visibility = View.INVISIBLE } just runs

        // When
        view.invisible()

        // Then
        verify(exactly = 1) { view.visibility = View.INVISIBLE }
    }

    @Test
    fun `if view is gone should set its visibility to invisible`() {
        // Given
        every { view.visibility } returns View.GONE
        every { view.visibility = View.INVISIBLE } just runs

        // When
        view.invisible()

        // Then
        verify(exactly = 1) { view.visibility = View.INVISIBLE }
    }

    @Test
    fun `if view is already invisible should not set its visibility again on invisible`() {
        // Given
        every { view.visibility } returns View.INVISIBLE

        // When
        view.invisible()

        // Then
        verify(exactly = 0) { view.visibility = any() }
    }

    // toBitmap

    @Test
    fun `if view has a size should create an ARGB_8888 bitmap with the width and height of the view`() {
        // Given
        val fakeWidth = 320
        val fakeHeight = 480
        every { view.width } returns fakeWidth
        every { view.height } returns fakeHeight
        every { Bitmap.createBitmap(fakeWidth, fakeHeight, Bitmap.Config.ARGB_8888) } returns bitmap
        every { view.draw(any()) } just runs

        // When
        view.toBitmap()

        // Then
        verify(exactly = 1) { Bitmap.createBitmap(fakeWidth, fakeHeight, Bitmap.Config.ARGB_8888) }
    }

    @Test
    fun `if bitmap is created should return the same bitmap`() {
        // Given
        val fakeWidth = 111
        val fakeHeight = 222
        every { view.width } returns fakeWidth
        every { view.height } returns fakeHeight
        every { Bitmap.createBitmap(fakeWidth, fakeHeight, Bitmap.Config.ARGB_8888) } returns bitmap
        every { view.draw(any()) } just runs

        // When
        val result = view.toBitmap()

        // Then
        assertSame(bitmap, result)
    }

    @Test
    fun `if bitmap is created should draw the view once on a canvas after creating the bitmap`() {
        // Given
        val fakeWidth = 64
        val fakeHeight = 48
        every { view.width } returns fakeWidth
        every { view.height } returns fakeHeight
        every { Bitmap.createBitmap(fakeWidth, fakeHeight, Bitmap.Config.ARGB_8888) } returns bitmap
        every { view.draw(any()) } just runs

        // When
        view.toBitmap()

        // Then
        verify(exactly = 1) { view.draw(any<Canvas>()) }
        verifyOrder {
            Bitmap.createBitmap(fakeWidth, fakeHeight, Bitmap.Config.ARGB_8888)
            view.draw(any())
        }
    }
}
