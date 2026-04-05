package org.kotlinlsp.common

fun getLspVersion(): String {
    val pkg = object {}.javaClass.`package`
    return pkg.implementationVersion ?: "unknown"
}