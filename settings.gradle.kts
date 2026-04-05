pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    	maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin")
        
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        maven(url = "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")
        maven(url = "https://repo.gradle.org/gradle/libs-releases")
        maven { url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/" }
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
        maven("https://cache-redirector.jetbrains.com/kotlin.bintray.com/kotlin-plugin")
        
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        maven(url = "https://www.jetbrains.com/intellij-repository/releases")
        maven(url = "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")
        maven(url = "https://repo.gradle.org/gradle/libs-releases")
        maven { url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/" }
    }
}

rootProject.name = "KotlinIDE"

include(
  ":app",
  ":kotlinc",
  ":code-editor",
  ":lsp",
  ":java-stubs"
)
