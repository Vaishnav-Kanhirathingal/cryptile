package com.example.cryptile.ui_fragments.prompt

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

private const val TAG = "Biometrics"

object Biometrics {
    /**
     * function to verify biometrics.
     * @param [onSuccess] action to be performed on Success.
     * @param [onFailure] action to be performed if the verification fails completely.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun verifyBiometrics(
        context: Context,
        description: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        // TODO: fix issues
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationFailed() {
                //no issues here
                super.onAuthenticationFailed()
                Log.d(TAG, "onAuthenticationFailed called")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                //no issues here
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "onAuthenticationSucceeded called")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                //gets called as soon as the prompt is displayed and with cancel button
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "onAuthenticationError called, $errorCode $errString")
                onFailure()
            }
        }

        val cancellationSignal = CancellationSignal()
        cancellationSignal.setOnCancelListener {
            //doesn't get called after cancel is clicked on the biometrics prompt
            Log.d(TAG, "cancellationSignal called")
            Toast.makeText(context, "action cancelled", Toast.LENGTH_SHORT).show()
            onFailure()
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
                cancellationSignal,
                context.mainExecutor,
                authenticationCallback
            )
    }
}