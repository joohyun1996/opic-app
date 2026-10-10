plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
// 여러 기능이 같이 쓰는 화면 부품 (TASK 19 발음 힌트부터, 디자인 개편 때 공용 테마도 여기로)
android {
    namespace = "com.jooh.opic.core.ui"
    compileSdk = 37
    defaultConfig { minSdk = 34 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:common"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
}
