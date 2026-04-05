package dev.mutwakil.kotlinide.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.kotlinlsp.lsp.KotlinLanguageServer
import org.kotlinlsp.lsp.KotlinLanguageServerNotifier
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors

object LSPManager {

    // جعل العميل والسيرفر متاحين بشكل static للوصول إليهما من أي مكان
    @JvmField
    var lspClient: KotlinLSPClient? = null
    
    @JvmField
    var lspServer: KotlinLanguageServer? = null

    private val executor = Executors.newFixedThreadPool(4)

    /**
     * تشغيل السيرفر والعميل وعمل الربط (Setup) بينهما
     */
    @JvmStatic
    fun setupLSP() {
        if (lspClient != null) return // منع التشغيل المتكرر

        // 1. إنشاء العميل
        val client = KotlinLSPClient()
        
        // 2. إعداد الأنابيب (Pipes) للاتصال الداخلي (In-Memory)
        val clientIn = PipedInputStream()
        val serverOut = PipedOutputStream(clientIn)

        val serverIn = PipedInputStream()
        val clientOut = PipedOutputStream(serverIn)

        // 3. إنشاء السيرفر مع المنبه (Notifier)
        val serverNotifier = object : KotlinLanguageServerNotifier {
            override fun onExit() {
                android.util.Log.d("LSP", "Server Exited")
            }
            override fun onBackgroundIndexingFinished() {
                android.util.Log.d("LSP", "Indexing Finished")
            }
        }
        
        val server = KotlinLanguageServer(serverNotifier)

        // 4. إطلاق الـ Launcher للعميل
        val launcher = LSPLauncher.createClientLauncher(
            client, 
            clientIn, 
            clientOut, 
            executor
        ) { it }

        // 5. ربط السيرفر بالـ Proxy الخاص بالعميل
        server.connect(launcher.remoteProxy)
        
        // 6. بدء الاستماع (Listening) في خلفية التطبيق
        launcher.startListening()

        // تخزين النسخ لاستخدامها لاحقاً
        this.lspClient = client
        this.lspServer = server
        
        android.util.Log.d("LSP", "LSP Server and Client are ready!")
    }
}
