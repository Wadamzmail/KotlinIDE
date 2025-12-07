package com.mutwakil.lsp.kotlin;

import android.app.Service;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.javacs.kt.KotlinLanguageServer; // تأكد من استيراد كلاس السيرفر الصحيح

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KotlinLspService extends Service {

    private static final String TAG = "KotlinLspService";
    private static final String SOCKET_NAME = "kotlin-lsp"; // اسم القناة
    
    private LocalServerSocket serverSocket;
    private Thread serverThread;
    private Future<Void> listeningFuture;

    @Override
    public IBinder onBind(Intent intent) {
        return null; // لا نحتاج لربط مباشر (Binding)، سنتواصل عبر السوكت
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting Kotlin LSP Service...");

        if (serverThread != null && serverThread.isAlive()) {
            return START_STICKY;
        }

        serverThread = new Thread(() -> {
            try {
                // 1. إنشاء السوكت المحلي
                // نقوم بمسح أي سوكت قديم بنفس الاسم لتجنب تضارب العناوين
                serverSocket = new LocalServerSocket(SOCKET_NAME);
                Log.d(TAG, "Listening on LocalSocket: " + SOCKET_NAME);

                // 2. انتظار اتصال المحرر (Blocking call)
                LocalSocket clientSocket = serverSocket.accept();
                Log.d(TAG, "Client connected!");

                // 3. تشغيل الـ LSP
                try {
                    // إنشاء نسخة من سيرفر Kotlin
                    KotlinLanguageServer server = new KotlinLanguageServer();

                    // ربط الـ Streams
                    // input للسيرفر هو output العميل، والعكس صحيح، لكن هنا نمرر streams السوكت مباشرة
                    Launcher<LanguageClient> launcher = Launcher.createLauncher(
                            server,
                            LanguageClient.class,
                            clientSocket.getInputStream(),
                            clientSocket.getOutputStream()
                    );

                    // توصيل السيرفر بالعميل (LSP4J Proxy)
                    server.connect(launcher.getRemoteProxy());

                    // بدء الاستماع (Blocking call until exit)
                    listeningFuture = launcher.startListening();
                    listeningFuture.get(); // الانتظار حتى ينتهي السيرفر

                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "LSP Execution Error", e);
                } finally {
                    // تنظيف
                    if (clientSocket != null) clientSocket.close();
                }

            } catch (IOException e) {
                Log.e(TAG, "Socket Error", e);
            } finally {
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // إغلاق السيرفر عند تدمير الخدمة
        if (listeningFuture != null) {
            listeningFuture.cancel(true);
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
