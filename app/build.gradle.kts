plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

android {
    namespace = "com.geely.ex2.range"
    compileSdk = 35

    val versionPropsFile = file("version.properties")
    val versionProps = Properties().also { props ->
        versionPropsFile.inputStream().use { props.load(it) }
    }
    val appVersionName = versionProps.getProperty("VERSION_NAME", "0.1.0")
    val appVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()

    defaultConfig {
        applicationId = "com.geely.ex2.range"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * `user` — sideload / debug.
     * `system` — sharedUserId=android.uid.system + platform-подпись (как Tools / CentralEXAuto),
     *           иначе CAR_* на ГУ часто не выдаются.
     */
    flavorDimensions += "install"
    productFlavors {
        create("user") {
            dimension = "install"
            isDefault = true
        }
        create("system") {
            dimension = "install"
            versionNameSuffix = "-system"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
