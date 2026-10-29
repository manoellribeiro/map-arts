package manoellribeiro.dev.martp.core.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SketchArtTypeTest {

    // zoomToSliderPosition

    @Test
    fun `if zoom is the min zoom should return zero on zoomToSliderPosition`() {
        // Given
        val fakeArtStyle = SketchArtType.COLORFUL_MAP

        // When
        val result = fakeArtStyle.zoomToSliderPosition(16F)

        // Then
        assertEquals(0F, result)
    }

    @Test
    fun `if zoom is the max zoom should return one on zoomToSliderPosition`() {
        // Given
        val fakeArtStyle = SketchArtType.COLORFUL_MAP

        // When
        val result = fakeArtStyle.zoomToSliderPosition(17F)

        // Then
        assertEquals(1F, result)
    }

    @Test
    fun `if zoom is between min and max zoom should return its proportional position on zoomToSliderPosition`() {
        // Given
        val fakeArtStyle = SketchArtType.COLORFUL_MAP

        // When
        val result = fakeArtStyle.zoomToSliderPosition(16.25F)

        // Then
        assertEquals(0.25F, result)
    }

    @Test
    fun `if zoom is below the min zoom should return zero on zoomToSliderPosition`() {
        // Given
        val fakeArtStyle = SketchArtType.COLORFUL_MAP

        // When
        val result = fakeArtStyle.zoomToSliderPosition(10F)

        // Then
        assertEquals(0F, result)
    }

    @Test
    fun `if zoom is above the max zoom should return one on zoomToSliderPosition`() {
        // Given
        val fakeArtStyle = SketchArtType.COLORFUL_MAP

        // When
        val result = fakeArtStyle.zoomToSliderPosition(20F)

        // Then
        assertEquals(1F, result)
    }

    // sliderPositionToZoom

    @Test
    fun `if slider position is zero should return the min zoom on sliderPositionToZoom`() {
        // Given
        val fakeArtStyle = SketchArtType.DEFAULT

        // When
        val result = fakeArtStyle.sliderPositionToZoom(0F)

        // Then
        assertEquals(14F, result)
    }

    @Test
    fun `if slider position is one should return the max zoom on sliderPositionToZoom`() {
        // Given
        val fakeArtStyle = SketchArtType.DEFAULT

        // When
        val result = fakeArtStyle.sliderPositionToZoom(1F)

        // Then
        assertEquals(15F, result)
    }

    @Test
    fun `if slider position is between zero and one should return the proportional zoom on sliderPositionToZoom`() {
        // Given
        val fakeArtStyle = SketchArtType.DEFAULT

        // When
        val result = fakeArtStyle.sliderPositionToZoom(0.25F)

        // Then
        assertEquals(14.25F, result)
    }

    @Test
    fun `if art style changes should keep the same slider position`() {
        // Given
        val fakePreviousArtStyle = SketchArtType.COLORFUL_MAP
        val fakeNewArtStyle = SketchArtType.DEFAULT

        // When
        val sliderPosition = fakePreviousArtStyle.zoomToSliderPosition(16.25F)
        val result = fakeNewArtStyle.sliderPositionToZoom(sliderPosition)

        // Then
        assertEquals(14.25F, result)
    }
}
