package com.music.spotui.ui.components

import app.myspoti.api.Image
import app.myspoti.api.best
import org.junit.Assert.*
import org.junit.Test

class ArtworkSelectionTest {
    @Test fun variantsAreOrderIndependentAndMatchSurfaceBudget() {
        val variants = listOf(Image("thumb", 64), Image("hero", 640), Image("row", 300))
        for (images in listOf(variants, variants.reversed(), variants.drop(1) + variants.first())) {
            assertEquals("row", images.best(300)?.url)
            assertEquals("hero", images.best(640)?.url)
        }
    }
    @Test fun tinyOnlyIsPlaceholderNotUpscaledArtwork() {
        assertNull(listOf(Image("thumb", 64)).best(300))
        assertNull(listOf(Image("thumb", 64)).best(640))
        assertEquals("thumb", listOf(Image("thumb", 64)).best(64)?.url)
        assertEquals("unknown", listOf(Image("unknown")).best(640)?.url)
    }
}
