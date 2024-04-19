plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.iqchannels.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.iqchannels.example"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
    //implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":iqchannels-sdk"))

    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
//    implementation 'com.google.android.gms:play-services:12.0.1'
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.drawerlayout:drawerlayout:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.1.0")
    implementation("androidx.core:core-ktx:1.12.0")

    testImplementation("junit:junit:4.12")
//    androidTestImplementation('androidx.test.espresso:espresso-core:3.1.0', {
//        exclude group: 'com.android.support', module: 'support-annotations'
//    })
}
