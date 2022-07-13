package com.example.cryptile.ui_fragments.prompt

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.util.Log
import android.widget.Toast

private const val TAG = "Biometrics"

object Biometrics {
    fun verifyBiometrics(
        context: Context,
        description: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "onAuthenticationFailed called")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "onAuthenticationSucceeded called")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "onAuthenticationError called")
                onFailure()
            }
        }
        BiometricPrompt
            .Builder(context)
            .setTitle("Authenticate")
            .setSubtitle("Scan fingerprint to proceed")
            .setDescription(description)
            .setNegativeButton(
                "cancel",
                context.mainExecutor
            ) { _, _ -> }
            .build()
            .authenticate(
                getCancellationSignal(context, onFailure),
                context.mainExecutor,
                authenticationCallback
            )
    }

    private fun getCancellationSignal(context: Context, onFailure: () -> Unit): CancellationSignal {
        val cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            Log.d(TAG, "cancellationSignal called")
            Toast.makeText(context, "action cancelled", Toast.LENGTH_SHORT).show()
            onFailure()
        }
        return cancellationSignal
    }
}