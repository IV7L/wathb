package com.yawmiyati.app

import android.app.Application
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.auth.TokenManager

class YawmiyatiApp : Application() {
    lateinit var tokenManager: TokenManager
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        ApiClient.init(tokenManager)
        instance = this
    }

    companion object {
        lateinit var instance: YawmiyatiApp
            private set
    }
}
