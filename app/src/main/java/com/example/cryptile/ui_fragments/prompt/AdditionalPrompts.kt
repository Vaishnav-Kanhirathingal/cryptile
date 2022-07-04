package com.example.cryptile.ui_fragments.prompt

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.cryptile.databinding.PromptLoadingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AdditionalPrompts"

object AdditionalPrompts {
    private lateinit var loadingBinding: PromptLoadingBinding
    private lateinit var dialog: Dialog

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

    fun initializeLoading(
        layoutInflater: LayoutInflater,
        context: Context,
        title: String
    ) {
        loadingBinding = PromptLoadingBinding.inflate(layoutInflater)
        dialog = Dialog(context)
        dialog.apply {
            setContentView(loadingBinding.root)
            window!!.setLayout(
                MATCH_PARENT,
                WRAP_CONTENT
            )
            setCancelable(false)
            show()
            Log.d(TAG, "show loading")
        }
        loadingBinding.progressTitleTextView.text = title
    }

    fun addProgress(
        progress: Int,
        dismiss: Boolean
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            if (dismiss) {
                dialog.dismiss()
            } else {
                loadingBinding.progressBar.progress = progress
            }
        }
    }

    fun checkAccountPassword(
        layoutInflater: LayoutInflater,
        context: Context,
        title: String,
        onSuccess: () -> Unit
    ) {
        // TODO: check user
    }
}