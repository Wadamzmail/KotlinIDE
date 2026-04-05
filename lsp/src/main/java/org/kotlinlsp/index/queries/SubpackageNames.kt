package org.kotlinlsp.index.queries

import org.kotlinlsp.index.Index

fun Index.subpackageNames(fqName: String): Set<String> = query { db ->
    db.packagesDb.prefixSearchRaw(fqName)
        .filter { (key, _) -> key != fqName }
        .map { (key, _) -> key.removePrefix("$fqName.").split(".") }
        .filter { it.isNotEmpty() }
        .map { it.first() }
        .toSet()
}
