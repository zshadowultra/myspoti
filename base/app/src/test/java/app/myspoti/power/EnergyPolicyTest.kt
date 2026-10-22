package app.myspoti.power

import org.junit.Assert.*
import org.junit.Test

class EnergyPolicyTest {
    @Test fun constructorDefaultsMatchResolvedNormalPolicy() {
        assertEquals(EnergyPolicy(), EnergyPolicy.resolve(false, false))
        assertFalse(EnergyState().canvasEnabled)
        assertFalse(EnergyState().policy.allowCanvas)
    }

    @Test fun visualPolicyMatrixPreservesAmoledAndSeparateVideos() {
        for (manual in listOf(false, true)) for (black in listOf(false, true)) {
            val off = EnergyPolicy.resolve(manual, black)
            val on = EnergyPolicy.resolve(manual, black, canvasEnabled = true)
            assertFalse(off.allowCanvas)
            assertEquals(!manual, on.allowCanvas)
            // The Canvas switch changes no other feature, including official videos.
            assertEquals(off, on.copy(allowCanvas = false))
            assertEquals(black, off.oledBlack)
            assertEquals(manual, off.saverEnabled)
            assertEquals(!manual, off.allowBackgroundLookups)
            assertEquals(if (manual) 480 else 1080, off.videoMaxHeight)
            assertEquals(if (manual) 20 else 30, off.progressFps)
            assertEquals(if (manual) 60f else 0f, off.displayRefreshCap, 0f)
        }
    }

    @Test fun androidSaverIsNotFollowed() {
        assertFalse(EnergyState().policy.saverEnabled)
    }

    @Test fun turningAmoledOffKeepsTheSameSaverRestrictions() {
        val enabled = EnergyState(manualSaver = true, blackPreference = true, canvasEnabled = true)
        val disabled = enabled.copy(blackPreference = false)
        assertTrue(enabled.policy.oledBlack)
        assertFalse(disabled.policy.oledBlack)
        assertEquals(enabled.policy.copy(oledBlack = false), disabled.policy)
    }

    @Test fun leavingSaverRestoresOnlyExplicitlyEnabledCanvas() {
        assertFalse(EnergyState().policy.allowCanvas)
        assertTrue(EnergyState(canvasEnabled = true).policy.allowCanvas)
        assertFalse(EnergyState(manualSaver = true, canvasEnabled = true).policy.allowCanvas)
    }
}
