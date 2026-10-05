plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.jooh.opic.core.llm"
    compileSdk = 37
    defaultConfig { minSdk = 34 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
}
