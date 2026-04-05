package org.kotlinlsp.index

import org.kotlinlsp.analysis.modules.Module
import org.kotlinlsp.analysis.modules.asFlatSequence
import org.kotlinlsp.index.worker.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

class ScanFilesThread(
    private val worker: WorkerThread,
    private val modules: List<Module>
) : Runnable {
    private val shouldStop = AtomicBoolean(false)

    override fun run() {

        // Scan phase
        modules.asFlatSequence()
            .filter { it.isSourceModule }
            .map { it.computeFiles(extended = true) }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                val command = Command.ScanSourceFile(it)
                worker.submitCommand(command)
            }

        worker.submitCommand(Command.SourceScanningFinished)

        // Once scanning is done and the analysis API is available, index all files (Index phase)
        modules.asFlatSequence()
            // Scan source files first as they will be more frequently accessed by the user
            .sortedByDescending { it.isSourceModule }
            .map { it.computeFiles(extended = true) }
            .flatten()
            .takeWhile { !shouldStop.get() }
            .forEach {
                worker.submitCommand(Command.IndexFile(it))
            }

        worker.submitCommand(Command.IndexingFinished)
    }

    fun signalToStop() {
        shouldStop.set(true)
    }
}
