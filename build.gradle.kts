plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.axion.release)
}

scmVersion {
    tag {
        prefix.set("v")
    }
}

val resolvedScmVersion: String = scmVersion.version

allprojects {
    version = resolvedScmVersion
}
