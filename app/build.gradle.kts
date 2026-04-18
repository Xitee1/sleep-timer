plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Resolves the app version from the current git tag.
// CI tag pushes use GITHUB_REF_NAME; locally we accept HEAD only if it sits exactly
// on a tag, so dev builds always fall through to "0.0.0-dev" (versionCode 1).
// Tag format: "vMAJOR.MINOR.PATCH" with optional "-suffix" (e.g. v1.0.0-beta).
// versionCode schema: major * 100000 + minor * 1000 + patch * 10.
fun resolveAppVersion(): Pair<String, Int> {
    val tag = if (System.getenv("GITHUB_REF_TYPE") == "tag") {
        System.getenv("GITHUB_REF_NAME")
    } else {
        runCatching {
            val process = ProcessBuilder("git", "describe", "--tags", "--exact-match", "HEAD")
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && out.isNotBlank()) out else null
        }.getOrNull()
    }

    if (tag == null) return "0.0.0-dev" to 1

    val name = tag.removePrefix("v")
    val semver = name.substringBefore("-").split(".").mapNotNull { it.toIntOrNull() }
    require(semver.size >= 3) { "Tag '$tag' is not in expected vMAJOR.MINOR.PATCH[-suffix] form" }
    val code = (semver[0] * 100000 + semver[1] * 1000 + semver[2] * 10).coerceAtLeast(1)
    return name to code
}

val (appVersionName, appVersionCode) = resolveAppVersion()

android {
    namespace = "dev.xitee.sleeptimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.xitee.sleeptimer"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
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
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // shizuku-provider is declared in this module's AndroidManifest (<provider>),
    // so the artifact must be directly visible on :app's compile classpath for lint
    // to find the class. core:service only uses shizuku-api.
    implementation(libs.shizuku.provider)
}
