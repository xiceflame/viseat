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
        // Rokid Maven 仓库（CXR-M SDK）
        maven {
            url = uri("https://maven.rokid.com/repository/maven-public/")
        }
        // JitPack 仓库（MPAndroidChart 等第三方库）
        maven {
            url = uri("https://jitpack.io")
        }
        mavenCentral()
    }
}

rootProject.name = "RokidNutritionPhone"
include(":app")
