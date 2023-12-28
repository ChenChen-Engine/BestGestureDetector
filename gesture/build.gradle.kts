plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.chenchen-engine"
            afterEvaluate { from(components["release"]) }
        }
    }
}


android {
    namespace = "chenchen.engine.gesture"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:1.9.10")
}