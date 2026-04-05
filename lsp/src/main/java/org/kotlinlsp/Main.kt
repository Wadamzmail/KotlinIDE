package org.kotlinlsp

import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.common.getLspVersion
import org.kotlinlsp.common.profileJvmStartup
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.util.concurrent.Executors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    profileJvmStartup()
    if ("-v" in args || "--version" in args) {
        println(getLspVersion())
        return
    }

    val notifier = object : KotlinLanguageServerNotifier {
        override fun onExit() {
            exitProcess(0)
        }
    }
    val executor = Executors.newSingleThreadExecutor {
        Thread(it, "KotlinLSP-LSP")
    }
    val server = KotlinLanguageServer(notifier)
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out, executor) { it }

    server.connect(launcher.remoteProxy)
    launcher.startListening()
}
