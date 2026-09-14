import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.jadval.shahr"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.jadvalian.qyuzwr"
    minSdk = 24
    targetSdk = 36
    versionCode = 13
    versionName = "2.2"
  }

  signingConfigs {
    create("release") {
      // Credentials come from local.properties (gitignored) or environment variables.
      // No secrets are stored in version control.
      val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
      }
      fun secret(envKey: String, propKey: String): String =
        System.getenv(envKey) ?: localProps.getProperty(propKey)
        ?: throw GradleException("Missing signing secret: set $envKey env var or $propKey in local.properties")

      storeFile = file(secret("KEYSTORE_PATH", "KEYSTORE_PATH"))
      storePassword = secret("KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
      keyAlias = secret("KEY_ALIAS", "KEY_ALIAS")
      keyPassword = secret("KEY_PASSWORD", "KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // ⚠️ IMPORTANT: sign debug builds with the SAME release key (salari.jks) so a
      // debug APK can be installed as an UPDATE over the release version (and vice versa)
      // WITHOUT uninstalling. Uninstalling wipes all user progress (Room DB + coins).
      signingConfig = signingConfigs.getByName("release")
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
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.adivery)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.coil.compose)
  debugImplementation(libs.androidx.compose.ui.tooling)
  ksp(libs.androidx.room.compiler)
}
