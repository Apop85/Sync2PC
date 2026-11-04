package com.sync2pc.app.data

import com.sync2pc.app.smb.SmbTarget

/**
 * Zugangsdaten zum Übernehmen (0.10.0): Profile mit gleichem Benutzer, gleicher Domäne, gleichem Server und
 * gleichem Passwort ergeben einen einzigen Eintrag. [source] = erstes Profil der Gruppe (liefert die Werte).
 */
data class CredentialGroup(
    val user: String,
    val domain: String,
    /** Server aus dem Freigabe-Pfad, leer wenn keiner eingetragen ist */
    val host: String,
    val profileNames: List<String>,
    val source: Profile,
)

object CredentialGroups {
    /** [passwordOf] entschlüsselt das Passwort eines Profils (in Tests ein einfacher Ersatz) */
    fun group(profiles: List<Profile>, passwordOf: (Profile) -> String): List<CredentialGroup> =
        profiles.filter { it.smbUser.isNotBlank() }
            .groupBy { p ->
                listOf(
                    p.smbUser.trim().lowercase(), p.smbDomain.trim().lowercase(),
                    hostOf(p).lowercase(), passwordOf(p),
                )
            }
            .values.map { list ->
                val first = list.first()
                CredentialGroup(
                    user = first.smbUser.trim(), domain = first.smbDomain.trim(), host = hostOf(first),
                    profileNames = list.map { it.name.trim() }, source = first,
                )
            }
            .sortedWith(compareBy({ it.host.lowercase() }, { it.user.lowercase() }))

    private fun hostOf(p: Profile): String = SmbTarget.parse(p.smbPath)?.host.orEmpty()
}
