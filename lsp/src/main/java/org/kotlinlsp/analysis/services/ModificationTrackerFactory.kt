package org.kotlinlsp.analysis.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerByEventFactoryBase

// TODO May need implementation for module changes (e.g. adding a file)
class ModificationTrackerFactory(project: Project) : KotlinModificationTrackerByEventFactoryBase(project)