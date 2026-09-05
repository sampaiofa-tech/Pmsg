@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.lifecycle.viewmodel.compose.kmp)
            implementation(libs.lifecycle.runtime.compose.kmp)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor3)
            implementation(libs.qrose)
        }

        val nonWebMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
            }
        }

        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.ai)
            implementation(libs.firebase.appcheck.recaptcha)
            implementation(libs.firebase.appcheck.debug)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.logging.interceptor)
            implementation(libs.retrofit)
            implementation(libs.converter.moshi)
            implementation(libs.moshi.kotlin)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.coil.compose)
            implementation(libs.bouncycastle.bcprov)
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
        }

        sourceSets.named("androidMain") {
            dependsOn(nonWebMain)
        }

        sourceSets.named("desktopMain") {
            dependsOn(nonWebMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.bouncycastle.bcprov)
                implementation("net.java.dev.jna:jna:5.14.0")
                implementation("net.java.dev.jna:jna-platform:5.14.0")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        listOf("iosX64Main", "iosArm64Main", "iosSimulatorArm64Main").forEach { targetName ->
            sourceSets.findByName(targetName)?.dependsOn(iosMain)
        }

        sourceSets.named("wasmJsMain") {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        sourceSets.named("androidUnitTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.core)
            }
        }
    }
}

extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "tech.sampaiofa.raix"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val hasReleaseKeystore = keystorePropertiesFile.exists()
    val isReleaseTaskRequested = gradle.startParameter.taskNames.any {
        it.contains("release", ignoreCase = true)
    }

    if (!hasReleaseKeystore && isReleaseTaskRequested) {
        throw GradleException(
            "ERRO CRÍTICO DE SEGURANÇA: 'keystore.properties' não encontrado na raiz do projeto! " +
            "A compilação de release foi bloqueada para impedir assinatura acidental com debug."
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                val properties = Properties()
                keystorePropertiesFile.reader(Charsets.UTF_8).use { reader ->
                    properties.load(reader)
                }
                fun getProp(key: String): String {
                    return (properties.getProperty(key)
                        ?: properties.getProperty("\uFEFF$key")
                        ?: properties.entries.firstOrNull { (it.key as? String)?.trim()?.removePrefix("\uFEFF") == key }?.value as? String)
                        ?: throw GradleException("Propriedade '$key' ausente em keystore.properties")
                }
                val storeFilePath = getProp("storeFile")
                storeFile = file(storeFilePath)
                storePassword = getProp("storePassword")
                keyAlias = getProp("keyAlias")
                keyPassword = getProp("keyPassword")
            }
        }
        val customDebugKeystore = file("${rootDir}/debug.keystore")
        if (customDebugKeystore.exists()) {
            create("debugConfig") {
                storeFile = customDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            signingConfigs.findByName("debugConfig")?.let {
                signingConfig = it
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions { unitTests { isIncludeAndroidResources = true } }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

compose.desktop {
    application {
        mainClass = "com.example.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.AppImage)
            packageName = "Raix"
            packageVersion = "1.5.0"
            description = "Raix - Mensageiro Efêmero e Criptografado (Zero-Trace)"
            copyright = "© 2026 Raix"
            vendor = "Raix"
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.ERROR }

dependencies {
    "kspAndroid"(libs.androidx.room.compiler)
    "kspAndroid"(libs.moshi.kotlin.codegen)
}
