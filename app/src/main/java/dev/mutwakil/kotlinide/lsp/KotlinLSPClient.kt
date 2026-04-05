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
    
    // واجهات لربط أحداث السيرفر بواجهة أندرويد
    var diagnosticsListener: ((PublishDiagnosticsParams) -> Unit)? = null
    var progressListener: ((ProgressParams) -> Unit)? = null

    /**
     * تشغيل السيرفر المدمج وربطه بالعميل مباشرة
     */
    fun startAndConnect() {
        // 1. إعداد الأنابيب (Pipes) للاتصال الداخلي
        val clientIn = PipedInputStream()
        val serverOut = PipedOutputStream(clientIn)

        val serverIn = PipedInputStream()
        val clientOut = PipedOutputStream(serverIn)

        // 2. إنشاء السيرفر مع المنبه الخاص به
        val notifier = object : KotlinLanguageServerNotifier {
            override fun onExit() { /* التعامل مع الإغلاق */ }
            override fun onBackgroundIndexingFinished() { /* تحديث الـ UI */ }
        }
        
        server = KotlinLanguageServer(notifier)

        // 3. إطلاق الـ Client Launcher (يستمع إلى clientIn ويرسل عبر clientOut)
        val launcher = LSPLauncher.createClientLauncher(
            this, 
            clientIn, 
            clientOut, 
            clientExecutor
        ) { it }

        // 4. ربط السيرفر بالـ Client
        server.connect(launcher.remoteProxy)
        
        // 5. بدء الاستماع في خلفية التطبيق
        launcher.startListening()

        // ملاحظة: في حالة الـ Embedded، السيرفر يحتاج أيضاً لمن يقرأ من serverIn
        // LSP4J تتعامل مع هذا داخلياً عند بدء تشغيل الـ Server Launcher إذا لزم الأمر
    }

    // --- استقبال البيانات من السيرفر (Callbacks) ---

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        // أخطاء الكود تصل هنا (مثلاً: خط أحمر تحت الكلمة)
        diagnosticsListener?.invoke(diagnostics)
    }

    override fun notifyProgress(params: ProgressParams) {
        // تحديث شريط التحميل أثناء الفهرسة (Indexing)
        progressListener?.invoke(params)
    }

    override fun logMessage(message: LogMessageParams) {
        android.util.Log.i("LSP_LOG", "[${message.type}] ${message.message}")
    }

    override fun showMessage(message: ShowMessageParams) {}
    override fun showMessageRequest(p: ShowMessageRequestParams) = null
    override fun telemetryEvent(obj: Any) {}

    // --- طلبات من الأندرويد إلى السيرفر ---

    fun initServer(rootPath: String): CompletableFuture<InitializeResult> {
        val params = InitializeParams().apply {
            workspaceFolders = listOf(WorkspaceFolder("file://$rootPath"))
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
