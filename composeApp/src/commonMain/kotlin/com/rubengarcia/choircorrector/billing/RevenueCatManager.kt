package com.rubengarcia.choircorrector.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RevenueCatManager {

    suspend fun isProActive(): Boolean =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfo(
                onError = { error ->
                    continuation.resumeWithException(
                        IllegalStateException(error.message)
                    )
                },
                onSuccess = { customerInfo: CustomerInfo ->
                    continuation.resume(
                        customerInfo.entitlements.active.containsKey("choir_corrector_pro")
                    )
                }
            )
        }
}
