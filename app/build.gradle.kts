import java.util.Properties

plugins {
    id("com.android.application")
    // Kotlin ist seit AGP 9 eingebaut (kein org.jetbrains.kotlin.android mehr)
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// --- Versionierung (siehe version.properties) ---------------------------------
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val vMajor = versionProps.getProperty("VERSION_MAJOR").trim().toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR").trim().toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH").trim().toInt()
// GitHub-Repo für Selbst-Updates, z. B. "benutzer/Sync2PC" (leer = in der App einstellen)
val updateRepo = versionProps.getProperty("UPDATE_REPO", "").trim()

// --- Signierung (keystore.properties wird vom Builder erzeugt, nicht in Git) ---
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "com.sync2pc.app"
    // 0.9.5: Android 16 (API 36), vom Play Store für neue Apps und Updates verlangt
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sync2pc.app"
        minSdk = 29
        targetSdk = 36
        versionCode = vMajor * 1_000_000 + vMinor * 1_000 + vPatch
        versionName = "$vMajor.$vMinor.$vPatch"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: Code verkleinern + ungenutzte Ressourcen entfernen (Keep-Regeln in proguard-rules.pro)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        // Kotlin übernimmt jvmTarget automatisch von targetCompatibility (built-in Kotlin)
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Migrationstest (0.9.4): MigrationTestHelper liest die exportierten Schemas aus den Test-Assets
    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*",
                "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF", "META-INF/INDEX.LIST",
                "META-INF/AL2.0", "META-INF/LGPL2.1"
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Room liest und schreibt das Schema (app/schemas/…/<Version>.json) in Debug UND Release. Laufen beide KSP-Schritte
// gleichzeitig, liest der eine evtl. eine gerade erst angelegte, noch leere Datei
// ("Expected start of the object '{', but had 'EOF'", so beim ersten Build von DB-Version 4). Darum nacheinander.
tasks.matching { it.name == "kspReleaseKotlin" }.configureEach { mustRunAfter("kspDebugKotlin") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Room 2.7+: nötig für KSP2 (KSP 2.3 kennt nur noch KSP2)
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // SMB2/SMB3 Client
    implementation("com.hierynomus:smbj:0.13.0")
    // Freigaben eines PCs auflisten (0.11.0, Windows-Dienst srvsvc über die smbj-Verbindung)
    implementation("com.rapid7.client:dcerpc:0.12.13") {
        exclude(group = "com.hierynomus", module = "smbj") // eigene smbj-Version oben
        exclude(group = "org.bouncycastle")                // kommt schon über smbj
    }

    testImplementation("junit:junit:4.13.2")
    // org.json gibt es auf Android eingebaut; für Unit-Tests auf dem PC die echte Bibliothek
    testImplementation("org.json:json:20240303")

    // Instrumentierter Migrationstest (0.9.4, läuft auf Gerät/Emulator: gradlew connectedDebugAndroidTest)
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    // WorkManager-Testtreiber nur für instrumentierte Tests (SYNC-20261111-163 B1/B5, Entscheid Architecture -183): kein Robolectric, gleiche Version wie work-runtime-ktx
    androidTestImplementation("androidx.work:work-testing:2.10.0")
}
