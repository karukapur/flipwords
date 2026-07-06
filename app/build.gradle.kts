plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.samsungzh"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.samsungzh"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.20")
            because("The app compiles with Kotlin 2.0.20; newer transitive Kotlin metadata crashes the compiler.")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    runtimeOnly("com.google.ai.edge.litertlm:litertlm-android:0.13.1") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
