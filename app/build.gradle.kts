import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.landpoint.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.landpoint.app"
        minSdk = 24
        targetSdk = 35
        // CI (release.yml) passes -PlpVersionCode=<commit count> and
        // -PlpVersionName=<tag> so the numbers follow the history; a local
        // invocation without them falls back to these.
        versionCode = (project.findProperty("lpVersionCode") as String?)?.toInt() ?: 10
        versionName = (project.findProperty("lpVersionName") as String?) ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "in")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // MapLibre and SQLCipher both ship native code for four ABIs, and a single APK
    // carrying all eight libraries is roughly twice the size any one phone can use.
    // Split them, and keep a universal APK as well: these builds are sideloaded from
    // a GitHub release, so somebody who does not know their ABI needs one download
    // that simply works. versionCode is deliberately the SAME across every split —
    // there is no Play Store here to order them, and distinct codes would make
    // swapping a per-ABI APK for the universal one look like a downgrade.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
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
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        // AGP 8 stores .so files raw so the installer can mmap straight out of the
        // APK, which is the right trade for a Play download. These builds are
        // sideloaded whole over whatever connection the user has, so compressing
        // MapLibre's and SQLCipher's native libraries — together the bulk of the
        // download — matters more than the milliseconds saved at install time.
        jniLibs.useLegacyPackaging = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true

        unitTests.all {
            // /tmp here is a tmpfs — RAM, shared with everything else on the box.
            // Robolectric unpacks a native runtime per SDK level into java.io.tmpdir,
            // which failed with "No space left on device" and spent memory the build
            // is deliberately conserving. Real disk instead.
            it.systemProperty(
                "java.io.tmpdir",
                rootProject.layout.buildDirectory.dir("test-tmp").get().asFile
                    .also { dir -> dir.mkdirs() }.absolutePath
            )
            it.maxHeapSize = "1024m"
        }
    }

    androidResources {
        generateLocaleConfig = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

// Style gate: ktlint reads its code style and rule overrides from the committed
// .editorconfig (ktlint_code_style = android_studio), so a run in CI and a run
// anywhere else can never disagree about what "clean" means.
ktlint {
    version.set(libs.versions.ktlintCli.get())
}

// Code-smell gate. The config only turns off rules that cannot be satisfied
// here; everything else fires, and pre-existing findings live in the committed
// baseline so the gate only ever blocks NEW issues.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Encrypts the database at rest. Consumer ProGuard rules come with the AAR, so
    // R8 keeps the native entry points without anything added here.
    implementation(libs.sqlcipher.android)

    implementation(libs.androidx.datastore.preferences)
    // Back-ports the Android 12 splash screen to Android 7 (Nougat), which is
    // now the minimum.
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)
    // Fingerprint or screen-lock confirmation for the app lock. Pulls in
    // androidx.fragment, which is why MainActivity is a FragmentActivity.
    implementation(libs.androidx.biometric)
    implementation(libs.coil.compose)
    // The map engine. Vector tiles, a style JSON the app can generate and store
    // locally, and an offline region downloader — all without an API key and
    // without Google Play Services.
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
