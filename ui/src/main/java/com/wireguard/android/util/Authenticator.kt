/*
 * Copyright © 2017-2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.os.Handler
import android.util.Log
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.wireguard.android.R

internal class Authenticator(
        private val fragmentActivity: FragmentActivity,
        private val callback: (AuthenticationResult) -> Unit
) {

    private val handler = Handler()
    private val biometricManager = BiometricManager.from(fragmentActivity)

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Log.d(TAG, "Error: $errorCode: $errString")
            callback(
                    when (errorCode) {
                        BiometricConstants.ERROR_USER_CANCELED -> AuthenticationResult.Cancelled
                        else -> AuthenticationResult.UnrecoverableError(errorCode, errString)
                    }
            )
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.d(TAG, "Failed")
            callback(AuthenticationResult.Failure)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            Log.d(TAG, "Success")
            callback(AuthenticationResult.Success(result.cryptoObject))
        }
    }

    private val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            { runnable -> handler.post(runnable) },
            authCallback
    )

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(fragmentActivity.getString(R.string.biometric_prompt_title))
            .setDeviceCredentialAllowed(true)
            .build()

    fun authenticate() {
        if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            callback(AuthenticationResult.UnrecoverableError(0, fragmentActivity.getString(R.string.biometric_prompt_no_hardware)))
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    companion object {
        private const val TAG = "WireGuard/Authenticator"
    }
}
