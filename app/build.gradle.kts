plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.kotlin.compose)
}

apksign {
    storeFileProperty = "androidStoreFile"
    storePasswordProperty = "androidStorePassword"
    keyAliasProperty = "androidKeyAlias"
    keyPasswordProperty = "androidKeyPassword"
}

val repo = jgit.repo()
val commitCount = (repo?.commitCount("refs/remotes/origin/main") ?: 1)
val latestTag = repo?.latestTag?.removePrefix("v") ?: "1.0"

android {
    namespace = "com.parallelc.micts"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        versionCode = commitCount
        versionName = latestTag

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "app"

    productFlavors {
        create("MiCTS") {
            dimension = "app"
            applicationId = "com.parallelc.micts"
            buildConfigField("String", "APP_NAME", "\"MiCTS\"")
        }

        create("VISTrigger") {
            dimension = "app"
            applicationId = "com.parallelc.vistrigger"
            resValue("string", "app_name", "VISTrigger")
            resValue("string", "tile_label", "VIS")
            resValue("string", "xposed_description", "Trigger Voice Interaction Service on any Android 9–15 device")
            buildConfigField("String", "APP_NAME", "\"VISTrigger\"")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "MiCTS_${variant.versionName}_${variant.versionCode}_${variant.baseName}.apk"
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.animation.core.android)
    implementation(libs.animation.android)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.accompanist.drawablepainter)
    compileOnly(project(":libxposed-compat"))
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.hiddenapibypass)
}