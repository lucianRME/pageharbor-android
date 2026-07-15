plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun runGitCommand(vararg args: String): String? {
    if (!rootProject.layout.projectDirectory.file(".git").asFile.exists()) return null

    return runCatching {
        val process = ProcessBuilder("git", *args)
            .directory(rootProject.layout.projectDirectory.asFile)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (process.waitFor() == 0 && output.isNotBlank()) output else null
    }.getOrNull()
}

fun gitRevisionForDebugBuild(): String {
    val revision = runGitCommand("rev-parse", "--short=7", "HEAD") ?: return "unknown"
    val hasChanges = runGitCommand("status", "--porcelain").orEmpty().isNotBlank()
    return if (hasChanges) "$revision-dirty" else revision
}

android {
    namespace = "org.synapseworks.pageharbor"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.synapseworks.pageharbor"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_REVISION", "\"unknown\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "GIT_REVISION", "\"${gitRevisionForDebugBuild()}\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.mlkit.document.scanner)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
