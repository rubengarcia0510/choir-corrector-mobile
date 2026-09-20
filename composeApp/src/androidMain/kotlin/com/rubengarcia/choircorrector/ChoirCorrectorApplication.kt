package com.rubengarcia.choircorrector

import android.app.Application
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

class ChoirCorrectorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Purchases.logLevel = LogLevel.DEBUG

        check(BuildConfig.REVENUECAT_TEST_STORE_API_KEY.isNotBlank()) {
            "RevenueCat Test Store API key is not configured"
        }

        Purchases.configure(
            apiKey = BuildConfig.REVENUECAT_TEST_STORE_API_KEY
        )
    }
}
