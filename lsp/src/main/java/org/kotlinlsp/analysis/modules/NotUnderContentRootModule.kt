package org.kotlinlsp.analysis.modules

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

@OptIn(KaExperimentalApi::class, KaPlatformInterface::class)
internal class NotUnderContentRootModule(
    override val name: String,
    override val directRegularDependencies: List<KaModule> = emptyList(),
    override val directDependsOnDependencies: List<KaModule> = emptyList(),
    override val directFriendDependencies: List<KaModule> = emptyList(),
    override val targetPlatform: TargetPlatform = JvmPlatforms.defaultJvmPlatform,
    override val file: PsiFile? = null,
    override val moduleDescription: String,
    override val project: Project,
) : KaNotUnderContentRootModule, KaModuleBase() {
    override val baseContentScope: GlobalSearchScope =
        if (file != null) GlobalSearchScope.fileScope(file) else GlobalSearchScope.EMPTY_SCOPE
}
