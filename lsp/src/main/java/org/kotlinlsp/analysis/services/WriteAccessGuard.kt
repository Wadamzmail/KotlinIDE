package org.kotlinlsp.analysis.services

import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.editor.Document

class WriteAccessGuard: DocumentWriteAccessGuard() {
    override fun isWritable(p0: Document): Result {
        return success()
    }
}
