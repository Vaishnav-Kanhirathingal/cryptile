package com.example.cryptile.ui_fragments.prompt

import android.app.AlertDialog
import android.content.Context
import android.util.Log

private const val TAG = "AdditionalPrompts"

object AdditionalPrompts {

    fun confirmationPrompt(
        context: Context,
        title: String,
        message: String,
        onSuccess: () -> Unit
    ) {
        Log.d(TAG, "title : $title, message: $message")
        val dialogBox = AlertDialog.Builder(context)
        dialogBox.apply {
            setTitle(title)
            setMessage(message)
            setCancelable(true)
            setNegativeButton("No") { _, _ -> }
            setPositiveButton("Yes") { _, _ -> onSuccess() }
            show()
        }
    }
}