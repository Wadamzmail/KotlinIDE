package org.kotlinlsp.analysis.modules

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

data class SerializedModule(
    val id: String,
    val dependencies: List<String>,
    val contentRoots: List<String>,
    val javaVersion: String,
    val isSource: Boolean,
    // SourceModule
    val kotlinVersion: String? = null,
    // LibraryModule
    val isJdk: Boolean? = null,
)

fun serializeModules(modules: List<Module>): String {
    val visited = LinkedHashMap<String, SerializedModule>()
    val stack = ArrayDeque<Module>()
    stack.addAll(modules)

    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        val id = current.id
        if(visited.containsKey(id)) continue

        visited[id] = when(current) {
            is SourceModule -> SerializedModule(
                id = id,
                contentRoots = current.contentRoots.map { it.absolutePathString() },
                kotlinVersion = current.kotlinVersion.versionString,
                javaVersion = current.javaVersion.toString(),
                dependencies = current.dependencies.map { it.id },
                isSource = current.isSourceModule
            )
            is LibraryModule -> SerializedModule(
                id = id,
                contentRoots = current.contentRoots.map { it.absolutePathString() },
                isJdk = current.isJdk,
                javaVersion = current.javaVersion.toString(),
                dependencies = current.dependencies.map { it.id },
                isSource = current.isSourceModule
            )
            else -> throw Exception("Unsupported KaModule!")
        }
        stack.addAll(current.dependencies)
    }

    return GsonBuilder().setPrettyPrinting().create().toJson(visited.values)
}

fun deserializeModules(
    data: String,
    appEnvironment: KotlinCoreApplicationEnvironment,
    project: Project
): List<Module> {
    val gson = Gson()
    val modules: List<SerializedModule> = gson.fromJson(data, Array<SerializedModule>::class.java).toList()
    val moduleMap = modules.associateBy { it.id }
    return buildModulesGraph(modules, moduleMap, appEnvironment, project)
}

fun buildModulesGraph(
    modules: List<SerializedModule>,
    moduleMap: Map<String, SerializedModule>,
    appEnvironment: KotlinCoreApplicationEnvironment,
    project: Project
): List<Module> {
    val builtModules = mutableMapOf<String, Module>()

    fun build(id: String): Module {
        if (builtModules.containsKey(id)) return builtModules[id]!!
        val serialized = moduleMap[id]!!
        val deps = serialized.dependencies.map { build(it) }
        val module = buildModule(serialized, deps, project, appEnvironment)
        builtModules[id] = module
        return module
    }

    return modules
        .asSequence()
        .filter { it.isSource }
        .map { build(it.id) }
        .toList()
}

private fun buildModule(
    it: SerializedModule,
    deps: List<Module>,
    project: Project,
    appEnvironment: KotlinCoreApplicationEnvironment
): Module =
    if(it.isSource) {
        SourceModule(
            id = it.id,
            kotlinVersion = LanguageVersion.fromVersionString(it.kotlinVersion!!)!!,
            javaVersion = JvmTarget.fromString(it.javaVersion)!!,
            contentRoots = it.contentRoots.map { Path(it) },
            dependencies = deps,
            project = project
        )
    } else {
        LibraryModule(
            id = it.id,
            javaVersion = JvmTarget.fromString(it.javaVersion)!!,
            isJdk = it.isJdk!!,
            contentRoots = it.contentRoots.map { Path(it) },
            dependencies = deps,
            project = project,
            appEnvironment = appEnvironment,
        )
    }
