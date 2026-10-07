plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.jooh.opic.core.stt"
    compileSdk = 37
    defaultConfig {
        minSdk = 34
        // S23+ 등 64비트 ARM만 대상으로 한다 (빌드 시간·APK 크기 절약)
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_static") } }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:llm")) // ModelSpec, HttpModelStore (모델 다운로드 재사용)
    implementation(libs.kotlinx.coroutines.android)
}
