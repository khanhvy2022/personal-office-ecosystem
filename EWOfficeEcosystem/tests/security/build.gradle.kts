plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.eurowindow.office.tests.security"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":engine:collabora-runtime"))
    implementation(project(":core:security"))
    implementation(project(":core:storage"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    testImplementation("junit:junit:4.13.2")
}
