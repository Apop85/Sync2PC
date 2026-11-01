// AGP 9: Kotlin ist eingebaut ("built-in Kotlin"), das Plugin org.jetbrains.kotlin.android entfällt.
// Die Kotlin-Version wird hier festgelegt; das Compose-Compiler-Plugin muss dieselbe Version haben.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
}
