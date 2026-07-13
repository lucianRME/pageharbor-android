package org.synapseworks.pageharbor

import org.junit.Assert.assertEquals
import org.junit.Test

class PageHarborBuildInfoTest {
    @Test
    fun applicationIdMatchesConfiguredValue() {
        assertEquals("org.synapseworks.pageharbor", BuildConfig.APPLICATION_ID)
    }
}
