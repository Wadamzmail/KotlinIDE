package org.kotlinlsp.index.db.adapters

import org.rocksdb.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class RocksDBAdapter(private val path: Path): DatabaseAdapter {
    companion object {
        init {
            RocksDB.loadLibrary()
        }

        val options = Options().apply {
            setCreateIfMissing(true)
            setKeepLogFileNum(1)
            setInfoLogLevel(InfoLogLevel.FATAL_LEVEL)
        }
    }

    private val db = RocksDB.open(options, path.absolutePathString())

    override fun putRawData(key: String, value: ByteArray) {
        db.put(key.toByteArray(), value)
    }

    override fun putRawData(values: Iterable<Pair<String, ByteArray>>) {
        val batch = WriteBatch()
        values.forEach { (key, value) ->
            batch.put(key.toByteArray(), value)
        }
        db.write(WriteOptions(), batch)
        batch.close()
    }

    override fun getRawData(key: String): ByteArray? {
        val data = db.get(key.toByteArray()) ?: return null
        return data
    }

    override fun prefixSearchRaw(prefix: String): Sequence<Pair<String, ByteArray>> = sequence {
        val readOptions = ReadOptions().setPrefixSameAsStart(true)

        readOptions.use {
            val iterator = db.newIterator(readOptions)
            iterator.seek(prefix.toByteArray())

            while (iterator.isValid) {
                val key = iterator.key()
                val keyString = key.toString(Charset.defaultCharset())
                if (!keyString.startsWith(prefix)) break

                yield(Pair(keyString, iterator.value()))
                iterator.next()
            }
        }
    }

    override fun remove(key: String) {
        db.delete(key.toByteArray())
    }

    override fun remove(keys: Iterable<String>) {
        val batch = WriteBatch()
        keys.forEach {
            batch.delete(it.toByteArray())
        }
        db.write(WriteOptions(), batch)
        batch.close()
    }

    override fun close() {
        db.close()
    }

    override fun deleteDb() {
        if(!db.isClosed) db.close()
        File(path.absolutePathString()).deleteRecursively()
    }
}
