plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val gitCommitCount: Provider<Int> = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toInt() }

android {
    namespace = "dev.xitee.sleeptimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.xitee.sleeptimer"
        minSdk = 26
        targetSdk = 36
        versionCode = gitCommitCount.get()
        versionName = project.version.toString()
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("SIGNING_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use release signing when SIGNING_KEYSTORE_PATH env var is provided (CI with secrets);
            // otherwise fall back to debug signing so local/CI builds without secrets still succeed.
            signingConfig = if (!System.getenv("SIGNING_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:service"))
    implementation(project(":feature:timer"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // shizuku-provider is declared in this module's AndroidManifest (<provider>),
    // so the artifact must be directly visible on :app's compile classpath for lint
    // to find the class. core:service only uses shizuku-api.
    implementation(libs.shizuku.provider)
}
