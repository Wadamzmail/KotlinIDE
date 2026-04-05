package org.kotlinlsp.analysis.services

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.kotlinlsp.common.trace

class JavaModuleAccessibilityChecker(
    private val javaModuleResolver: CliJavaModuleResolver,
): KotlinJavaModuleAccessibilityChecker {
    override fun checkAccessibility(
        useSiteFile: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?
    ): KotlinJavaModuleAccessibilityError? {
        trace("KotlinJavaModuleAccessibilityChecker.checkAccessibility")
        val accessError = javaModuleResolver.checkAccessibility(useSiteFile, referencedFile, referencedPackage)
        return accessError?.let(::convertAccessError)
    }

    private fun convertAccessError(accessError: JavaModuleResolver.AccessError): KotlinJavaModuleAccessibilityError =
        when (accessError) {
            is JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotReadUnnamedModule

            is JavaModuleResolver.AccessError.ModuleDoesNotReadModule ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotReadModule(accessError.dependencyModuleName)

            is JavaModuleResolver.AccessError.ModuleDoesNotExportPackage ->
                KotlinJavaModuleAccessibilityError.ModuleDoesNotExportPackage(accessError.dependencyModuleName)
        }
}
