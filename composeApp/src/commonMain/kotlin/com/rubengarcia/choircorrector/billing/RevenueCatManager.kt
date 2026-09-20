package com.rubengarcia.choircorrector.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.StoreProduct
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

    suspend fun restorePurchases(): Boolean =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
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

    suspend fun purchasePro(): Boolean =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getProducts(
                productIds = listOf("choir_corrector_pro_monthly"),
                onError = { error ->
                    continuation.resumeWithException(
                        IllegalStateException(error.message)
                    )
                },
                onSuccess = { products: List<StoreProduct> ->
                    val product = products.firstOrNull()

                    if (product == null) {
                        continuation.resumeWithException(
                            IllegalStateException("RevenueCat product not found")
                        )
                        return@getProducts
                    }

                    Purchases.sharedInstance.purchase(
                        storeProduct = product,
                        onError = { error, userCancelled ->
                            if (userCancelled) {
                                continuation.resumeWithException(
                                    IllegalStateException("Purchase cancelled")
                                )
                            } else {
                                continuation.resumeWithException(
                                    IllegalStateException(error.message)
                                )
                            }
                        },
                        onSuccess = { _, customerInfo ->
                            continuation.resume(
                                customerInfo.entitlements.active.containsKey(
                                    "choir_corrector_pro"
                                )
                            )
                        }
                    )
                }
            )
        }
}
