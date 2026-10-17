plugins { id("com.android.application") }
android {
    namespace = "com.ayuemin.agent"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ayuemin.agent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-debug"
    }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
