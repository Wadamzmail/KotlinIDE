pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    	maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots")
        maven(url = "https://repo.gradle.org/gradle/libs-releases")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    	maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots")
        maven(url = "https://repo.gradle.org/gradle/libs-releases")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    }
}

rootProject.name = "KotlinIDE"

include(
  ":app",
  ":kotlinc",
  ":code-editor",
  ":lsp"
)
