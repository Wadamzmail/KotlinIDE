package dev.mutwakil.lsp.kotlin;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;

public class MyKotlinClient implements LanguageClient {

    @Override
    public void telemetryEvent(Object object) {
        // ممكن تلغي أو تستخدمه لتحليل usage
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        // TODO: عرض الأخطاء في UI المحرر
        System.out.println("Diagnostics: " + diagnostics);
    }

    @Override
    public void logMessage(MessageParams message) {
        System.out.println("LSP LOG: " + message.getMessage());
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        System.out.println("MESSAGE: " + messageParams.getMessage());
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params) {
        return CompletableFuture.completedFuture(
            new MessageActionItem("OK")
        );
    }
}