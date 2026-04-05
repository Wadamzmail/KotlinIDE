package org.kotlinlsp.analysis.services

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleJavaAnnotationsProvider
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.kotlinlsp.common.trace

@OptIn(KaNonPublicApi::class)
class JavaModuleAnnotationsProvider(
    private val javaModuleResolver: CliJavaModuleResolver,
): KotlinJavaModuleJavaAnnotationsProvider {
    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        trace("getAnnotationsForModuleOwnerOfClass")
        return javaModuleResolver.getAnnotationsForModuleOwnerOfClass(classId)
    }
}
