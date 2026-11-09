package com.sync2pc.app

import com.sync2pc.app.data.CredentialGroups
import com.sync2pc.app.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

/** Zugangsdaten übernehmen (0.10.0): gleiche Zugangsdaten erscheinen nur einmal */
class CredentialGroupsTest {
    private fun p(id: Long, name: String, user: String, path: String, pw: String = "a", domain: String = "") =
        Profile(id = id, name = name, smbUser = user, smbPath = path, smbDomain = domain, smbPasswordEnc = pw)

    private fun group(list: List<Profile>) = CredentialGroups.group(list) { it.smbPasswordEnc }

    @Test fun sameUserServerAndPasswordIsOneEntry() {
        val g = group(listOf(
            p(1, "Fotos", "tester", "\\\\192.0.2.3\\Fotos"),
            p(2, "OrdnerTest", "Tester", "\\\\192.0.2.3\\Fotos\\test"),
            p(3, "Dokumente", "tester", "\\\\192.0.2.3\\Dokumente"),
        ))
        assertEquals(1, g.size)
        assertEquals("192.0.2.3", g.single().host)
        assertEquals(listOf("Fotos", "OrdnerTest", "Dokumente"), g.single().profileNames)
        assertEquals(1L, g.single().source.id)
    }

    @Test fun differentServerUserOrPasswordAreSeparate() {
        val g = group(listOf(
            p(1, "A", "leo", "\\\\pc1\\Fotos"),
            p(2, "B", "leo", "\\\\pc2\\Fotos"),
            p(3, "C", "anna", "\\\\pc1\\Fotos"),
            p(4, "D", "leo", "\\\\pc1\\Fotos", pw = "anders"),
        ))
        assertEquals(4, g.size)
    }

    @Test fun profilesWithoutUserAreIgnored() {
        assertEquals(0, group(listOf(p(1, "A", "", "\\\\pc1\\Fotos"))).size)
    }
}
