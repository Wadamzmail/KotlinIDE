package dev.mutwakil.kotlinide.lsp

import android.util.Log
import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors

object LSPManager {

    var client: KotlinLSPClient? = null
        private set

    private var launcherFuture: java.util.concurrent.Future<*>? = null

    private val executor = Executors.newCachedThreadPool()

    fun start(rootPath: String) {
        if (client != null) return

        // ========= client =========
        val lspClient = KotlinLSPClient()

        // ========= server =========
        val notifier = object : KotlinLanguageServerNotifier {
            override fun onExit() {
                Log.d("LSP", "Server exited")
            }

            override fun onBackgroundIndexingFinished() {
                Log.d("LSP", "Indexing finished")
            }
        }

        val server = KotlinLanguageServer(notifier)

        // ========= pipes =========
        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        // ========= launcher =========
        val launcher = LSPLauncher.createServerLauncher(
            server,
            serverIn,
            serverOut,
            executor
        ) { it }

        // مهم جداً: الربط الصحيح
        server.connect(launcher.remoteProxy)

        // ربط client بالـ proxy
        lspClient.server = launcher.remoteProxy

        // تشغيل listening
        launcherFuture = launcher.startListening()

        client = lspClient

        Log.d("LSP", "LSP started successfully")

        // ========= initialize =========
        lspClient.initialize(rootPath).thenAccept {
            lspClient.initialized()
        }
    }

    fun stop() {
        client?.shutdown()
        launcherFuture?.cancel(true)
        executor.shutdownNow()
        client = null
    }
}