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
        versionCode = 1
        versionName = "0.1.0-starter"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
