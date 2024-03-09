plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "publish.gradle")

android {
    namespace = "ru.iqchannels.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val release by getting {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.fragment:fragment:1.3.6")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")

    implementation("com.google.code.gson:gson:2.8.5")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.squareup.okhttp3:okhttp:3.12.13")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("com.google.firebase:firebase-messaging:21.0.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    testImplementation("junit:junit:4.12")

}
