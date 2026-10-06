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
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(project(":feature:words"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:llm"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
}

val bundledWords = tasks.register<Sync>("bundleWords") {
    from(rootProject.file("exports/words.json"))
    into(layout.buildDirectory.dir("generated/wordAssets"))
}
android.sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/wordAssets").get().asFile)
tasks.named("preBuild").configure { dependsOn(bundledWords) }
