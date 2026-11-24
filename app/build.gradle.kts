plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.communitycare.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.communitycare.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.5-preview"
        manifestPlaceholders["appAuthRedirectScheme"] = "communitycare"
        buildConfigField("String", "API_BASE_URL", "\"https://care-api.95.111.247.118.sslip.io/\"")
        buildConfigField("String", "OIDC_ISSUER", "\"https://care-login.95.111.247.118.sslip.io/realms/communitycare\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"communitycare-android\"")
        buildConfigField("String", "OIDC_REDIRECT_URI", "\"communitycare://oauth/callback\"")
    }

    buildTypes {
        release {
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
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
}
