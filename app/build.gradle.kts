plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dle_prototype"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dle_prototype"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("org.tensorflow:tensorflow-lite:2.15.0")
    implementation("commons-io:commons-io:2.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.13.0") // if using ops
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}