plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.flowspark.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flowspark.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 通过 BuildConfig 注入构建期配置（真实 Key 走代理层，绝不下发）
        buildConfigField("String", "AI_PROXY_BASE_URL", "\"https://flowspark-proxy.example.workers.dev\"")
        buildConfigField("String", "AI_PROXY_API_KEY", "\"demo-key-please-configure\"")
        buildConfigField("String", "DEFAULT_LLM_MODEL", "\"deepseek-chat\"")
        buildConfigField("String", "DEFAULT_IMAGE_MODEL", "\"black-forest-labs/FLUX.1-schnell\"")
        buildConfigField("String", "ACRA_URI", "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("androidx.room:room-common:2.6.1")
    ksp(libs.androidx.room.compiler)
    // KSP 处理器需要 room-compiler-processing 传递的 room-common 类路径
    ksp("androidx.room:room-compiler-processing:2.6.1")

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Network (OpenAI-Compatible 统一接口)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Image loading
    implementation(libs.coil.compose)

    // ACRA crash reporting (lightweight Crashlytics 替代)
    implementation(libs.acra.http)
    implementation(libs.acra.dialog)
    // WindowSizeClass (大屏适配)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
