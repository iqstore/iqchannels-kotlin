// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.1" apply false
    id("com.android.library") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.gms.google-services") version "4.3.14" apply false
    id("com.jfrog.bintray") version "1.7.3" apply false
    id("com.github.dcendents.android-maven") version "2.0" apply false

    `maven-publish`
}
