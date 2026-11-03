package com.sync2pc.app.backup

import com.sync2pc.app.data.FolderMapping
import com.sync2pc.app.data.MirrorState
import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SyncedFile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Inhalt einer Sicherung. Passwörter im Klartext (nur im Speicher), Schlüssel = Profil-ID aus der Sicherung. */
data class BackupContent(
    val appVersion: String,
    val createdAt: Long,
    /** Globale Einstellungen: Werte vom Typ Boolean, Int oder String */
    val settings: Map<String, Any>,
    val profiles: List<Profile>,
    val passwords: Map<Long, String>,
    val mappings: List<FolderMapping>,
    val synced: List<SyncedFile>,
    val mirrorStates: List<MirrorState>,
)

/** Ergebnis des Einlesens. [passwordOk]: null = Datei ohne Passwörter, false = Export-Passwort fehlt/falsch. */
data class ParsedBackup(val content: BackupContent, val encrypted: Boolean, val passwordOk: Boolean?)

class BackupException(message: String) : Exception(message)

/**
 * Dateiformat der Sicherung (JSON). Reine Kotlin/Java-Logik, damit sie per Unit-Test prüfbar ist.
 *
 * SMB-Passwörter stehen nur in der Datei, wenn ein Export-Passwort gesetzt ist. Dann sind sie mit
 * AES-256-GCM verschlüsselt; der Schlüssel entsteht per PBKDF2-HMAC-SHA256 aus dem Export-Passwort.
 * Ein verschlüsselter Prüfwert erkennt ein falsches Export-Passwort.
 */
object BackupCodec {
    const val FORMAT = "leosync-backup"
    /** Erhöhen, wenn sich das Format inkompatibel ändert; ältere Versionen müssen lesbar bleiben */
    const val VERSION = 1
    const val DEFAULT_ITERATIONS = 210_000
    private const val CHECK_PLAIN = "LeoSync"
    private const val GCM_IV = 12

    // ---- Schreiben ---------------------------------------------------------------------
    fun encode(
        c: BackupContent,
        exportPassword: String?,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("app", c.appVersion)
        root.put("created", c.createdAt)

        val key: SecretKey? = if (!exportPassword.isNullOrEmpty()) {
            val salt = ByteArray(16).also { random.nextBytes(it) }
            val k = deriveKey(exportPassword, salt, iterations)
            root.put("crypto", JSONObject().apply {
                put("kdf", "PBKDF2WithHmacSHA256")
                put("iterations", iterations)
                put("salt", b64(salt))
                put("check", encrypt(k, CHECK_PLAIN, random))
            })
            k
        } else null

        root.put("settings", JSONObject().apply { c.settings.forEach { (k, v) -> put(k, v) } })
        root.put("profiles", JSONArray().apply {
            for (p in c.profiles) {
                val o = profileToJson(p)
                val pw = c.passwords[p.id]
                if (key != null && !pw.isNullOrEmpty()) o.put("password", encrypt(key, pw, random))
                put(o)
            }
        })
        root.put("folderMappings", JSONArray().apply {
            for (m in c.mappings) put(JSONObject().apply {
                put("profileId", m.profileId); put("groupKey", m.groupKey)
                put("folderName", m.folderName); put("seen", m.seen)
            })
        })
        root.put("syncedFiles", JSONArray().apply {
            for (s in c.synced) put(JSONObject().apply {
                put("profileId", s.profileId); put("sourceKey", s.sourceKey); put("mediaUri", s.mediaUri)
                put("size", s.size); put("dateModified", s.dateModified)
                if (s.partialHash != null) put("partialHash", s.partialHash)
                put("remotePath", s.remotePath); put("syncedAt", s.syncedAt); put("verified", s.verified)
                // Lösch-Zustand wird bewusst nicht gesichert (gilt nur für das Gerät, auf dem gelöscht wurde)
            })
        })
        root.put("mirrorState", JSONArray().apply {
            for (s in c.mirrorStates) put(JSONObject().apply {
                put("profileId", s.profileId); put("relPath", s.relPath)
                put("localSize", s.localSize); put("localModified", s.localModified)
                put("remoteSize", s.remoteSize); put("remoteModified", s.remoteModified); put("syncedAt", s.syncedAt)
            })
        })
        return root.toString()
    }

    // ---- Lesen -------------------------------------------------------------------------
    /** Kurzer Blick in die Datei (für die Vorschau), ohne Passwörter zu entschlüsseln */
    fun isEncrypted(json: String): Boolean = root(json).has("crypto")

    fun parse(json: String, exportPassword: String?): ParsedBackup {
        val root = root(json)
        val version = root.optInt("version", 0)
        if (version < 1 || version > VERSION)
            throw BackupException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.backup_err_version, version, VERSION))

        var key: SecretKey? = null
        var passwordOk: Boolean? = null
        val crypto = root.optJSONObject("crypto")
        if (crypto != null) {
            passwordOk = false
            if (!exportPassword.isNullOrEmpty()) {
                val k = deriveKey(exportPassword, unb64(crypto.getString("salt")), crypto.getInt("iterations"))
                if (decryptOrNull(k, crypto.getString("check")) == CHECK_PLAIN) { key = k; passwordOk = true }
            }
        }

        val settings = LinkedHashMap<String, Any>()
        root.optJSONObject("settings")?.let { s -> s.keys().forEach { k -> settings[k] = s.get(k) } }

        val profiles = ArrayList<Profile>()
        val passwords = HashMap<Long, String>()
        root.optJSONArray("profiles")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val p = profileFromJson(o)
                profiles += p
                if (key != null && o.has("password")) decryptOrNull(key, o.getString("password"))?.let { passwords[p.id] = it }
            }
        }
        val mappings = objects(root.optJSONArray("folderMappings")).map {
            FolderMapping(it.getLong("profileId"), it.getString("groupKey"), it.getString("folderName"), it.optBoolean("seen", false))
        }
        val synced = objects(root.optJSONArray("syncedFiles")).map {
            SyncedFile(
                profileId = it.getLong("profileId"), sourceKey = it.getString("sourceKey"),
                mediaUri = it.optString("mediaUri", ""), size = it.getLong("size"),
                dateModified = it.getLong("dateModified"),
                partialHash = if (it.has("partialHash")) it.getString("partialHash") else null,
                remotePath = it.optString("remotePath", ""), syncedAt = it.optLong("syncedAt", 0),
                verified = it.optBoolean("verified", false),
            )
        }
        val mirror = objects(root.optJSONArray("mirrorState")).map {
            MirrorState(
                it.getLong("profileId"), it.getString("relPath"), it.getLong("localSize"), it.getLong("localModified"),
                it.getLong("remoteSize"), it.getLong("remoteModified"), it.optLong("syncedAt", 0),
            )
        }
        val content = BackupContent(
            appVersion = root.optString("app", "?"), createdAt = root.optLong("created", 0),
            settings = settings, profiles = profiles, passwords = passwords,
            mappings = mappings, synced = synced, mirrorStates = mirror,
        )
        return ParsedBackup(content, encrypted = crypto != null, passwordOk = passwordOk)
    }

    private fun root(json: String): JSONObject {
        val r = try { JSONObject(json) } catch (e: Exception) { throw BackupException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.backup_err_json)) }
        if (r.optString("format") != FORMAT) throw BackupException(com.sync2pc.app.l10n.str(com.sync2pc.app.R.string.backup_err_format))
        return r
    }

    private fun objects(arr: JSONArray?): List<JSONObject> =
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.getJSONObject(it) }

    // ---- Profile ----------------------------------------------------------------------
    // Fehlende Felder (ältere Sicherungen) bekommen die Standardwerte der App.
    private fun profileToJson(p: Profile) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("enabled", p.enabled)
        put("smbPath", p.smbPath); put("smbUser", p.smbUser); put("smbDomain", p.smbDomain)
        put("sourceFolders", p.sourceFolders); put("wifiSsids", p.wifiSsids)
        put("includeImages", p.includeImages); put("includeVideos", p.includeVideos)
        put("includeSubfolders", p.includeSubfolders); put("deleteMode", p.deleteMode); put("verifyUpload", p.verifyUpload)
        put("profileType", p.profileType); put("direction", p.direction); put("localTreeUri", p.localTreeUri)
        put("remoteSubfolder", p.remoteSubfolder); put("propagateDeletes", p.propagateDeletes)
        put("conflictMode", p.conflictMode); put("includePatterns", p.includePatterns)
        put("excludePatterns", p.excludePatterns); put("remotePollMinutes", p.remotePollMinutes)
        put("trashRetentionDays", p.trashRetentionDays)
        put("massDeletePercent", p.massDeletePercent)
        put("keepVersions", p.keepVersions)
    }

    private fun profileFromJson(o: JSONObject): Profile {
        val d = Profile()
        return Profile(
            id = o.optLong("id", 0), name = o.optString("name", d.name), enabled = o.optBoolean("enabled", d.enabled),
            smbPath = o.optString("smbPath", d.smbPath), smbUser = o.optString("smbUser", d.smbUser),
            smbDomain = o.optString("smbDomain", d.smbDomain), smbPasswordEnc = "",
            sourceFolders = o.optString("sourceFolders", d.sourceFolders), wifiSsids = o.optString("wifiSsids", d.wifiSsids),
            includeImages = o.optBoolean("includeImages", d.includeImages), includeVideos = o.optBoolean("includeVideos", d.includeVideos),
            includeSubfolders = o.optBoolean("includeSubfolders", d.includeSubfolders),
            deleteMode = o.optString("deleteMode", d.deleteMode), verifyUpload = o.optBoolean("verifyUpload", d.verifyUpload),
            profileType = o.optString("profileType", d.profileType), direction = o.optString("direction", d.direction),
            localTreeUri = o.optString("localTreeUri", d.localTreeUri), remoteSubfolder = o.optString("remoteSubfolder", d.remoteSubfolder),
            propagateDeletes = o.optBoolean("propagateDeletes", d.propagateDeletes),
            conflictMode = o.optString("conflictMode", d.conflictMode),
            includePatterns = o.optString("includePatterns", d.includePatterns),
            excludePatterns = o.optString("excludePatterns", d.excludePatterns),
            remotePollMinutes = o.optInt("remotePollMinutes", d.remotePollMinutes),
            trashRetentionDays = o.optInt("trashRetentionDays", d.trashRetentionDays),
            massDeletePercent = o.optInt("massDeletePercent", d.massDeletePercent).coerceIn(1, 100),
            keepVersions = o.optBoolean("keepVersions", d.keepVersions),
        )
    }

    // ---- Kryptografie --------------------------------------------------------------------
    fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    /** "IV:Chiffrat" (beides Base64) */
    fun encrypt(key: SecretKey, plain: String, random: SecureRandom = SecureRandom()): String {
        val iv = ByteArray(GCM_IV).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return b64(iv) + ":" + b64(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    fun decryptOrNull(key: SecretKey, value: String): String? = try {
        val (iv, ct) = value.split(':', limit = 2).let { unb64(it[0]) to unb64(it[1]) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(cipher.doFinal(ct), Charsets.UTF_8)
    } catch (e: Exception) { null }

    private fun b64(b: ByteArray): String = Base64.getEncoder().encodeToString(b)
    private fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)
}
