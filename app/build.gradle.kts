plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chipstrap.rbx"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chipstrap.rbx"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.0.5"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing config: if a keystore file is present at app/release.keystore
    // (created by the CI workflow or supplied locally), sign with it; otherwise fall
    // back to the debug signing config so local `./gradlew assembleRelease` still works.
    //
    // Reads from env vars set by .github/workflows/release.yml:
    //   KS_PASS     — keystore store password
    //   KS_KEY_PASS — key password (falls back to KS_PASS if not set, mirrors keystore convention)
    //   KS_ALIAS    — key alias in the keystore (defaults to "chipstrap")
    signingConfigs {
        create("release") {
            val ksFile = rootProject.file("app/release.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KS_PASS") ?: "chipstrap-default-keystore-password"
                keyAlias = System.getenv("KS_ALIAS") ?: "chipstrap"
                keyPassword = System.getenv("KS_KEY_PASS") ?: System.getenv("KS_PASS") ?: "chipstrap-default-key-password"
            }
        }
    }

    buildTypes {
        release {
            // R8 minification is OFF for now — it was causing mysterious launch
            // crashes on real devices despite the app working fine in debug.
            // We'll re-enable it after we've added comprehensive keep rules.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Use the release signing config if its keystore exists, otherwise fall back to debug
            // so a plain `./gradlew assembleRelease` on a developer machine still produces an
            // installable APK (self-signed with the debug key).
            val ksFile = rootProject.file("app/release.keystore")
            signingConfig = if (ksFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions { unitTests { isReturnDefaultValues = true } }

    // D8/R8 dexer heap. The dexer runs in a worker JVM that defaults to a
    // small heap (~512m), which OOMs when merging external dex archives
    // (Shizuku SDK + AndroidX + Compose + Kotlin coroutines pull in a LOT of
    // classes). Bumping this to 2048m fixes the
    // "java.lang.OutOfMemoryError: GC overhead limit exceeded" error we saw
    // in CI on mergeExtDexDebug.
    @Suppress("DEPRECATION")
    dexOptions {
        javaMaxHeapSize = "2048m"
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)

    // Shizuku — lets us write ClientAppSettings.json into Roblox's private
    // /data/data directory without root, using the privileged binder.
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.shizuku.aidl)
    implementation(libs.shizuku.shared)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}
