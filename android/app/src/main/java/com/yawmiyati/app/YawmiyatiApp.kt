package com.yawmiyati.app

import android.app.Application
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.auth.TokenManager
import org.osmdroid.config.Configuration

class YawmiyatiApp : Application() {
    lateinit var tokenManager: TokenManager
        private set

    /** Populated by AppUpdateGate after config loads; read by home screens. */
    var supportWhatsapp: String = ""

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        ApiClient.init(tokenManager)
        instance = this
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }

    companion object {
        lateinit var instance: YawmiyatiApp
            private set
    }
}
