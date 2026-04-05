package org.kotlinlsp.index.queries

import org.kotlinlsp.index.Index
import org.kotlinlsp.index.db.Declaration
import org.kotlinlsp.index.db.adapters.prefixSearch

fun Index.getCompletions(prefix: String): Sequence<Declaration> = query {
    it.declarationsDb.prefixSearch<Declaration>(prefix)
        .map { (_, value) -> value }
}
