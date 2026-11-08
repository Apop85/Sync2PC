# R8-Regeln fuer Sync2PC (Release-Build mit Minify)
# Falls der Build mit "Missing class ..." abbricht: R8 schreibt Vorschlaege nach
# app/build/outputs/mapping/release/missing_rules.txt -> hier ergaenzen.

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions

# --- smbj (SMB-Client): nutzt Reflection und den Event-Bus mbassador ----------------
-keep class com.hierynomus.** { *; }
-keep class net.engio.mbassy.** { *; }
-keepclassmembers class * {
    @net.engio.mbassy.listener.Handler *;
}

# --- BouncyCastle (Kryptografie fuer SMB3/NTLM), wird per Provider-Name geladen ----
-keep class org.bouncycastle.** { *; }

# --- Auf Android nicht vorhandene, optionale Abhaengigkeiten ------------------------
-dontwarn org.slf4j.**
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.security.auth.kerberos.**
-dontwarn javax.naming.**
-dontwarn javax.annotation.**
-dontwarn java.lang.management.**
-dontwarn org.bouncycastle.**
-dontwarn com.hierynomus.**
-dontwarn net.engio.mbassy.**

# --- dcerpc (Freigaben auflisten, 0.11.0) und seine Hilfsbibliotheken ---------------
-keep class com.rapid7.client.dcerpc.** { *; }
-dontwarn com.rapid7.client.dcerpc.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn javax.lang.model.**
-dontwarn sun.misc.**

# --- Eigene Klassen ----------------------------------------------------------------
# Worker werden von WorkManager per Reflection erzeugt (Regeln kommen aus WorkManager),
# Receiver/Activity aus dem Manifest bleiben automatisch erhalten.
# Room-Entities werden ueber generierten Code angesprochen (kein Keep noetig).
