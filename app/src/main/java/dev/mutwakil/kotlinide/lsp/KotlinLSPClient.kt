package dev.mutwakil.kotlinide.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class KotlinLSPClient : LanguageClient {

    private lateinit var server: KotlinLanguageServer
    private val clientExecutor = Executors.newFixedThreadPool(4)
    
    var diagnosticsListener: ((PublishDiagnosticsParams) -> Unit)? = null
    var progressListener: ((ProgressParams) -> Unit)? = null

    fun startAndConnect() {
        val clientIn = PipedInputStream()
        val serverOut = PipedOutputStream(clientIn)

        val serverIn = PipedInputStream()
        val clientOut = PipedOutputStream(serverIn)

        val notifier = object : KotlinLanguageServerNotifier {
            override fun onExit() { }
            override fun onBackgroundIndexingFinished() { }
        }
        
        server = KotlinLanguageServer(notifier)

        // تعديل هنا: نحتاج لتعريف الـ Launcher بوضوح
        val launcher = LSPLauncher.createServerLauncher(
            server, 
            serverIn, 
            serverOut, 
            clientExecutor
        ) { it }

        // الخطأ كان هنا: يجب تمرير الـ RemoteProxy (الذي هو LanguageClient) للسيرفر
        // وليس السيرفر للعميل بشكل خاطئ
        server.connect(launcher.remoteProxy)
        
        launcher.startListening()
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        diagnosticsListener?.invoke(diagnostics)
    }

    override fun notifyProgress(params: ProgressParams) {
        progressListener?.invoke(params)
    }

    // تصحيح: LSP4J تستخدم MessageParams بدلاً من LogMessageParams في الإصدارات الحديثة
    override fun logMessage(params: MessageParams) {
        android.util.Log.i("LSP_LOG", "[${params.type}] ${params.message}")
    }

    override fun showMessage(params: MessageParams) {
        android.util.Log.i("LSP_SHOW", "[${params.type}] ${params.message}")
    }

    override fun showMessageRequest(params: ShowMessageRequestParams): CompletableFuture<MessageActionItem>? {
        return null
    }

    override fun telemetryEvent(obj: Any) {}

    fun initServer(rootPath: String): CompletableFuture<InitializeResult> {
        val params = InitializeParams().apply {
            // تصحيح: WorkspaceFolder يحتاج (uri, name) في بعض الإصدارات
            workspaceFolders = listOf(WorkspaceFolder("file://$rootPath", "root"))
        }
        return server.initialize(params)
    }

    fun onFileOpened(uri: String, content: String) {
        val params = DidOpenTextDocumentParams(TextDocumentItem(uri, "kotlin", 1, content))
        server.didOpen(params)
    }

    fun getCompletions(uri: String, line: Int, char: Int): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        val params = CompletionParams(TextDocumentIdentifier(uri), Position(line, char))
        return server.completion(params)
    }

    fun getHover(uri: String, line: Int, char: Int): CompletableFuture<Hover?> {
        return server.hover(HoverParams(TextDocumentIdentifier(uri), Position(line, char)))
    }

    fun dispose() {
        server.exit()
        clientExecutor.shutdown()
    }
}
