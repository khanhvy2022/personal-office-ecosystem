pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EWOfficeEcosystem"

include(":engine:collabora-runtime")
include(":core:security")
include(":core:storage")
include(":core:database")
include(":core:datastore")
include(":core:ui")
include(":app:documents")
include(":app:sheets")
include(":tests:integration")
include(":tests:compatibility")
include(":tests:security")
