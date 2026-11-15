plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
// Firebase is configured per team by placing app/google-services.json (not committed).
// Without it the app still builds; login screens show a setup notice and records stay on the device.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
android {
    namespace = "com.tinyrunners.school"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tinyrunners.school"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // FileProvider for the share cards (already brought in by Firebase at this version; named so it stays).
    implementation("androidx.core:core:1.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
