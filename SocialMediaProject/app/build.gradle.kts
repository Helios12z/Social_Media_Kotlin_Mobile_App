plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.socialmediaproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.socialmediaproject"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding=true
    }
}

dependencies {
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.database)
    implementation(libs.core.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.material3.android)
    implementation(libs.firebase.inappmessaging)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.androidx.core.ktx)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation ("com.google.code.gson:gson:2.12.1")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.github.yalantis:ucrop:2.2.9-native")
    implementation ("androidx.work:work-runtime-ktx:2.8.1")
    implementation ("com.onesignal:OneSignal:5.0.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.0")
}
