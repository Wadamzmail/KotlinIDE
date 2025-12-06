pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    	maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    	maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots")
    }
}

rootProject.name = "KotlinIDE"

include(
  ":app",
  ":kotlinc",
  ":lsp"
)
