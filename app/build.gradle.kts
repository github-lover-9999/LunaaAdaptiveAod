plugins {
    id("com.android.application")
}

android {
    namespace = "dev.lunaa.aod"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.lunaa.aod"
        minSdk = 26
        targetSdk = 36
        versionCode = 16500
        versionName = "1.6.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(project(":xposed-stubs"))
    testImplementation("junit:junit:4.13.2")
}
