plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.room.plugin)
}

android {
    namespace = "ch.nikleberg.bettershared"
    compileSdk = 34
    compileSdkExtension = 10

    defaultConfig {
        applicationId = "ch.nikleberg.bettershared"
        minSdk = 33
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

    packaging {
        resources {
            excludes += "/META-INF/{INDEX.LIST,AL2.0,LGPL2.1,io.netty.*}"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.microsoft.graph)
    implementation(libs.microsoft.identity)
    implementation(libs.room.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.navigation.fragment)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}