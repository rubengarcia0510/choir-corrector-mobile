plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()

    sourceSets {
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("io.ktor:ktor-client-okhttp:2.3.7")
        }

        commonMain.dependencies {
            implementation("com.revenuecat.purchases:purchases-kmp-core:3.7.0")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.rubengarcia.choircorrector"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.rubengarcia.choircorrector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("debug") {
            val revenueCatTestStoreApiKey =
                System.getenv("REVENUECAT_TEST_STORE_API_KEY")
                    ?: project.findProperty("REVENUECAT_TEST_STORE_API_KEY")?.toString()
                    ?: ""

            buildConfigField(
                "String",
                "REVENUECAT_TEST_STORE_API_KEY",
                "\"$revenueCatTestStoreApiKey\""
            )
        }
    }
}
