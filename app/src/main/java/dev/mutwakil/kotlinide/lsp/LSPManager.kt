package dev.mutwakil.kotlinide.lsp

import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors

object LSPManager {

    @JvmField
    var lspClient: KotlinLSPClient? = null
    
    @JvmField
    var lspServer: KotlinLanguageServer? = null

    private val executor = Executors.newFixedThreadPool(4)

    @JvmStatic
    fun setupLSP() {
        if (this.lspClient != null) return 

        // 1. إنشاء العميل والسيرفر ككائنات عادية أولاً
        val client = KotlinLSPClient()
        
        val serverNotifier = object : KotlinLanguageServerNotifier {
            override fun onExit() { }
            override fun onBackgroundIndexingFinished() { }
        }
        val server = KotlinLanguageServer(serverNotifier)

        // 2. إعداد الأنابيب للربط التبادلي
        // مخرج العميل يذهب لمدخل السيرفر
        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        // مخرج السيرفر يذهب لمدخل العميل
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        // 3. استخدام createServerLauncher كما في الـ Docs
        // البارامترات: (السيرفر، مدخل السيرفر، مخرج السيرفر، الـ executor، وظيفته)
        val launcher = LSPLauncher.createServerLauncher(
            server, 
            serverIn, 
            serverOut, 
            executor
        ) { it }

        // 4. الربط الحيوي:
        // السيرفر يحتاج الـ RemoteProxy (الذي هو واجهة العميل للطرف الآخر)
        server.connect(launcher.remoteProxy)
        
        // العميل يحتاج نسخة السيرفر لإرسال الطلبات (مثل initialize)
        client.setServer(server)

        // 5. بدء الاستماع من جهة السيرفر
        launcher.startListening()

        // 6. تشغيل Launcher للعميل أيضاً لاستقبال الردود
        val clientLauncher = LSPLauncher.createClientLauncher(
            client,
            clientIn,
            clientOut,
            executor
        ) { it }
        clientLauncher.startListening()

        this.lspClient = client
        this.lspServer = server
        
        android.util.Log.d("LSP", "LSP Bridge established using ServerLauncher")
    }
}
