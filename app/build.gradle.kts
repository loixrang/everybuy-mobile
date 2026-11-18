plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val productionApiBaseUrl = "https://api.everybuy.loixrang.com/api/v1/"
val versionCodeProperty = providers.gradleProperty("everybuy.versionCode").orNull
val appVersionCode = versionCodeProperty?.toIntOrNull()
    ?: if (versionCodeProperty == null) 1 else error("everybuy.versionCode must be an integer")
val appVersionName = providers.gradleProperty("everybuy.versionName").orElse("1.0").get()

require(appVersionCode in 1..2_100_000_000) {
    "everybuy.versionCode must be between 1 and 2100000000"
}
require(appVersionName.isNotBlank()) { "everybuy.versionName must not be empty" }

val releaseStoreFile = providers.environmentVariable("EVERYBUY_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("EVERYBUY_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("EVERYBUY_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("EVERYBUY_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        val missing = buildList {
            if (releaseStoreFile.isNullOrBlank()) add("EVERYBUY_STORE_FILE")
            else if (!file(releaseStoreFile).isFile) add("keystore file")
            if (releaseStorePassword.isNullOrBlank()) add("EVERYBUY_STORE_PASSWORD")
            if (releaseKeyAlias.isNullOrBlank()) add("EVERYBUY_KEY_ALIAS")
            if (releaseKeyPassword.isNullOrBlank()) add("EVERYBUY_KEY_PASSWORD")
        }
        check(missing.isEmpty()) {
            "Release signing is not configured (${missing.joinToString()}). " +
                "Set the Android signing environment variables; see README.md → Publishing releases."
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(validateReleaseSigning)
}

android {
    namespace = "com.everybuy.mobile"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.everybuy.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$productionApiBaseUrl\"")
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile.orEmpty())
                storePassword = releaseStorePassword.orEmpty()
                keyAlias = releaseKeyAlias.orEmpty()
                keyPassword = releaseKeyPassword.orEmpty()
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.window.size)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
