package dev.mutwakil.kotlinide.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

class KotlinLSPClient : LanguageClient {

    lateinit var server: LanguageServer

    var diagnosticsListener: ((PublishDiagnosticsParams) -> Unit)? = null
    var progressListener: ((ProgressParams) -> Unit)? = null

    // =========================
    // LSP أحداث جاية من السيرفر
    // =========================

    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        diagnosticsListener?.invoke(params)
    }

    override fun notifyProgress(params: ProgressParams) {
        progressListener?.invoke(params)
    }

    override fun logMessage(params: MessageParams) {
        android.util.Log.i("LSP_LOG", "[${params.type}] ${params.message}")
    }

    override fun showMessage(params: MessageParams) {
        android.util.Log.i("LSP_SHOW", "[${params.type}] ${params.message}")
    }

    override fun showMessageRequest(params: ShowMessageRequestParams)
        : CompletableFuture<MessageActionItem>? = null

    override fun telemetryEvent(obj: Any) {}

    // =========================
    // Requests (تمشي عبر proxy)
    // =========================

    fun initialize(rootPath: String): CompletableFuture<InitializeResult> {
        val params = InitializeParams().apply {
            workspaceFolders = listOf(
                WorkspaceFolder("file://$rootPath", "root")
            )
        }
        return server.initialize(params)
    }

    fun initialized() {
        server.initialized(InitializedParams())
    }

    fun didOpen(uri: String, content: String) {
        val params = DidOpenTextDocumentParams(
            TextDocumentItem(uri, "kotlin", 1, content)
        )
        server.textDocumentService.didOpen(params)
    }

    fun didChange(uri: String, version: Int, text: String) {
        val params = DidChangeTextDocumentParams().apply {
            textDocument = VersionedTextDocumentIdentifier(uri, version)
            contentChanges = listOf(
                TextDocumentContentChangeEvent(text)
            )
        }
        server.textDocumentService.didChange(params)
    }

    fun completion(
        uri: String,
        line: Int,
        char: Int
    ): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {

        val params = CompletionParams(
            TextDocumentIdentifier(uri),
            Position(line, char)
        )

        return server.textDocumentService.completion(params)
    }

    fun hover(uri: String, line: Int, char: Int)
        : CompletableFuture<Hover?> {

        return server.textDocumentService.hover(
            HoverParams(TextDocumentIdentifier(uri), Position(line, char))
        )
    }

    fun shutdown() {
        server.shutdown()
        server.exit()
    }
}