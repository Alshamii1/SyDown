plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ytdlp"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sydown.app"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("public") {
            dimension = "distribution"

            buildConfigField(
                "boolean",
                "IS_OWNER_BUILD",
                "false"
            )

            resValue(
                "string",
                "app_name",
                "SyDown"
            )
        }

        create("owner") {
            dimension = "distribution"
            applicationIdSuffix = ".owner"

            buildConfigField(
                "boolean",
                "IS_OWNER_BUILD",
                "true"
            )

            resValue(
                "string",
                "app_name",
                "SyDown Owner"
            )
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    /*
     * =====================================================
     * YT-DLP + FFMPEG
     * =====================================================
     */

    implementation(
        "io.github.junkfood02.youtubedl-android:library:0.18.1"
    )

    implementation(
        "io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1"
    )

    /*
     * =====================================================
     * COIL
     * =====================================================
     */

    implementation(
        "io.coil-kt.coil3:coil-compose:3.3.0"
    )

    implementation(
        "io.coil-kt.coil3:coil-network-okhttp:3.3.0"
    )

    /*
     * =====================================================
     * ANDROID / COMPOSE
     * =====================================================
     */

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    /*
     * =====================================================
     * TESTS
     * =====================================================
     */

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}