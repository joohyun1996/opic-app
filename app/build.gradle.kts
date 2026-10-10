plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.jooh.opic"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.jooh.opic"
        minSdk = 34
        targetSdk = 35
        versionCode = 27
        versionName = "0.27"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        // 개인 기기용: release도 이 Mac의 debug 키로 서명 → debug 빌드 위에 덮어 설치 가능(데이터 유지).
        // debug 빌드는 Compose 최적화가 꺼져 스크롤이 버벅인다 (Day 목록 늦은 프레임 13% → release 3%, 2026-10-10 측정)
        release { signingConfig = signingConfigs.getByName("debug") }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(project(":feature:words"))
    implementation(project(":feature:grammar"))
    implementation(project(":core:common"))
    implementation(project(":core:correction"))
    implementation(project(":feature:analysis"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:stt"))
    implementation(project(":core:ui"))
    implementation(project(":feature:shadowing"))
    implementation(project(":feature:speaking"))
    implementation(project(":core:llm"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
}

val bundledWords = tasks.register<Sync>("bundleWords") {
    from(rootProject.file("exports/words.json"))
    into(layout.buildDirectory.dir("generated/wordAssets"))
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/wordAssets").get().asFile)
tasks.named("preBuild").configure { dependsOn(bundledWords) }

val bundledGrammar = tasks.register<Sync>("bundleGrammar") {
    from(rootProject.file("exports/grammar.json"))
    from(rootProject.file("exports/grammar-core.json"))
    into(layout.buildDirectory.dir("generated/grammarAssets"))
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/grammarAssets").get().asFile)
tasks.named("preBuild").configure { dependsOn(bundledGrammar) }

val bundledSpeaking = tasks.register<Sync>("bundleSpeaking") {
    from(rootProject.file("exports/speaking.json"))
    from(rootProject.file("exports/templates.json"))
    into(layout.buildDirectory.dir("generated/speakingAssets"))
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/speakingAssets").get().asFile)
tasks.named("preBuild").configure { dependsOn(bundledSpeaking) }

val bundledShadowing = tasks.register<Sync>("bundleShadowing") {
    from(rootProject.file("exports/shadowing.json"))
    into(layout.buildDirectory.dir("generated/shadowingAssets"))
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/shadowingAssets").get().asFile)
tasks.named("preBuild").configure { dependsOn(bundledShadowing) }
