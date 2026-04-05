package org.kotlinlsp.index.worker

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.kotlinlsp.index.db.Database
import org.kotlinlsp.index.db.File
import org.kotlinlsp.index.db.file
import org.kotlinlsp.index.db.setFile

fun scanKtFile(project: Project, ktFile: KtFile, db: Database) {
    val newFile = File.fromKtFile(ktFile, project, indexed = false)

    val existingFile = db.file(newFile.path)
    if (
        File.shouldBeSkipped(existingFile = existingFile, newFile = newFile)
    ) return

    // Update the file timestamp and package
    db.setFile(newFile)
}
