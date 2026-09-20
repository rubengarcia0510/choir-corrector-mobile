package com.rubengarcia.choircorrector.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.awaitCustomerInfo

class RevenueCatManager {

    suspend fun isProActive(): Boolean {
        val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()

        return customerInfo
            .entitlements["choir_corrector_pro"]
            ?.isActive == true
    }
}
