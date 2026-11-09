plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Upload-key signing comes from env vars injected by scripts/bundle-release.sh
// (password from 1Password via `op run`); debug builds and tests don't need it.
val releaseSigning = ReleaseSigning.resolve(
    keystorePath = providers.environmentVariable(ReleaseSigning.KEYSTORE_ENV).orNull,
    password = providers.environmentVariable(ReleaseSigning.PASSWORD_ENV).orNull,
    fileExists = { file(it).isFile },
)
val configuredReleaseSigning = releaseSigning as? ReleaseSigning.Result.Configured

android {
    namespace = "com.factotum8.networking4jandroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.factotum8.networking4jandroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"
    }

    signingConfigs {
        if (configuredReleaseSigning != null) {
            create("release") {
                storeFile = file(configuredReleaseSigning.keystorePath)
                storePassword = configuredReleaseSigning.password
                keyAlias = ReleaseSigning.KEY_ALIAS
                keyPassword = configuredReleaseSigning.password
            }
        }
    }

    buildTypes {
        release {
            if (configuredReleaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Without the upload key, fail only the tasks that sign release artifacts
// (APK packaging / AAB signing) instead of shipping an unsigned build.
if (releaseSigning is ReleaseSigning.Result.Missing) {
    val message = releaseSigning.message
    val checkReleaseSigning = tasks.register("checkReleaseSigning") {
        doLast { throw GradleException(message) }
    }
    tasks.matching { it.name == "packageRelease" || it.name == "signReleaseBundle" }
        .configureEach { dependsOn(checkReleaseSigning) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
