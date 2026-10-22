plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.gps"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.gps"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// 可选：把高德 JAR 拷到 app/libs，便于部分 IDE 建立索引（执行 copyAmapSdkToLibs 后 Sync）
val amapLocalJar = layout.projectDirectory.file("libs/amap-3dmap-location-search.jar")

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid)
    if (amapLocalJar.asFile.exists()) {
        implementation(files(amapLocalJar))
    } else {
        implementation(libs.gaode.map.sdk)
    }
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register<Copy>("copyAmapSdkToLibs") {
    group = "ide"
    description = "Copy AMap SDK jar to app/libs for IDE indexing (optional)"
    from({
        configurations.getByName("debugCompileClasspath").files.firstOrNull { file ->
            file.name.startsWith("3dmap-location-search")
        } ?: throw GradleException(
            "AMap SDK not found. Sync Gradle first; need implementation(libs.gaode.map.sdk)."
        )
    })
    into(layout.projectDirectory.dir("libs"))
    rename { "amap-3dmap-location-search.jar" }
}
