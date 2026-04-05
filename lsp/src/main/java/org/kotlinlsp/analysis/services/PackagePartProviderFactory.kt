package org.kotlinlsp.analysis.services

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.kotlinlsp.common.trace

class PackagePartProviderFactory: KotlinPackagePartProviderFactory {
    private lateinit var allLibraryRoots: List<JavaRoot>

    fun setup(allLibraryRoots: List<JavaRoot>) {
        this.allLibraryRoots = allLibraryRoots
    }

    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        trace("createPackagePartProvider $scope")

        return JvmPackagePartProvider(latestLanguageVersionSettings, scope).apply {
            addRoots(allLibraryRoots, MessageCollector.NONE)
        }
    }
}
