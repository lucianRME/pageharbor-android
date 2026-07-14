package org.synapseworks.pageharbor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PageHarborBuildInfoTest {
    @Test
    fun applicationIdMatchesConfiguredValue() {
        assertEquals("org.synapseworks.pageharbor", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun versionMetadataMatchesConfiguredValue() {
        assertEquals("0.1.0-dev", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)
    }

    @Test
    fun gitRevisionMetadataIsPresentAndDoesNotExposeLocalPath() {
        assertFalse(BuildConfig.GIT_REVISION.isBlank())
        assertFalse(BuildConfig.GIT_REVISION.contains("/"))
        assertFalse(BuildConfig.GIT_REVISION.contains("\\"))
    }
}
