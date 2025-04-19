plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.opensafe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.opensafe"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.vision.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.google.mlkit:object-detection:17.0.2")
    implementation ("com.google.mlkit:object-detection-custom:17.0.2")
    implementation ("com.google.mlkit:object-detection:16.0.0")

    implementation ("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")

}