try {
    val env = System.getenv()
    val cl = env.javaClass
    val field = cl.getDeclaredField("m")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val writableEnv = field.get(env) as MutableMap<String, String>
    writableEnv.remove("ANDROID_PREFS_ROOT")
} catch (e: Exception) {
    // Ignore
}

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
