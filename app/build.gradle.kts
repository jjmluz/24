plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Packages the repo's solutions.json as an app asset, so there's only one copy of the database. */
abstract class CopyPuzzleDatabase : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        source.get().asFile.copyTo(outputDir.file("solutions.json").get().asFile, overwrite = true)
    }
}

val copyPuzzleDatabase = tasks.register<CopyPuzzleDatabase>("copyPuzzleDatabase") {
    source.set(rootProject.layout.projectDirectory.file("solutions.json"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyPuzzleDatabase, CopyPuzzleDatabase::outputDir)
    }
}

android {
    namespace = "com.jjmluz.twentyfour"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jjmluz.twentyfour"
        minSdk = 28
        targetSdk = 36
        // CI passes these for releases (-PversionCode=… -PversionName=…); local builds use the defaults.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse("0.1.0")
    }

    // CI signs releases with the keystore in the SIGNING_* secrets. Without them (local builds),
    // release falls back to the debug key, which is fine for sideloading but not for the Play Store.
    val releaseKeystore = providers.environmentVariable("SIGNING_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
