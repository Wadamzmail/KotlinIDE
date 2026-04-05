package org.kotlinlsp.common

import org.eclipse.lsp4j.DiagnosticSeverity
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity

fun KaSeverity.toLspSeverity(): DiagnosticSeverity =
    when(this) {
        KaSeverity.ERROR -> DiagnosticSeverity.Error
        KaSeverity.WARNING -> DiagnosticSeverity.Warning
        KaSeverity.INFO -> DiagnosticSeverity.Information
    }
