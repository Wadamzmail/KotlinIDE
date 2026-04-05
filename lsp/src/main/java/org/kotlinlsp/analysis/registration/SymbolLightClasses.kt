package org.kotlinlsp.analysis.registration

fun Registrar.symbolLightClasses() {
    projectService(
        "org.jetbrains.kotlin.asJava.KotlinAsJavaSupport",
        "org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport"
    )
}
