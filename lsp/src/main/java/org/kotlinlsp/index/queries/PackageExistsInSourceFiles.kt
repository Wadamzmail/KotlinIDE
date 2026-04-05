package org.kotlinlsp.index.queries

import org.jetbrains.kotlin.name.FqName
import org.kotlinlsp.index.Index

fun Index.packageExistsInSourceFiles(fqName: FqName): Boolean = query { db ->
    db.packagesDb.prefixSearchRaw(fqName.asString()).iterator().hasNext()
}
