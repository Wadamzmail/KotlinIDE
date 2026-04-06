package dev.mutwakil.kotlinide.lsp

import android.util.Log
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors

object LSPManager {

    var client: KotlinLSPClient? = null
        private set

    private val executor = Executors.newCachedThreadPool()

    fun start(rootPath: String) {
        if (client != null) return

        val lspClient = KotlinLSPClient()

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

        // ========= ServerLauncher =========
        val serverLauncher = LSPLauncher.createServerLauncher(
            server,
            serverIn,
            serverOut,
            executor
        ) { it }

        // السيرفر محتاج client proxy
        server.connect(serverLauncher.remoteProxy)

        // ========= ClientLauncher =========
        val clientLauncher = LSPLauncher.createClientLauncher(
            lspClient,
            clientIn,
            clientOut,
            executor
        ) { it }

        // دي النقطة المهمة 👇
        val serverProxy: LanguageServer = clientLauncher.remoteProxy

        lspClient.server = serverProxy

        // تشغيل الاتنين
        serverLauncher.startListening()
        clientLauncher.startListening()

        client = lspClient

        Log.d("LSP", "LSP started successfully")

        // initialize
        lspClient.initialize(rootPath).thenAccept {
            lspClient.initialized()
        }
    }

    fun stop() {
        client?.shutdown()
        executor.shutdownNow()
        client = null
    }
}