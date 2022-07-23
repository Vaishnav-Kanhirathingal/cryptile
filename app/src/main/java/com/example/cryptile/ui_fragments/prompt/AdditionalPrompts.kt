package com.example.cryptile.ui_fragments.prompt

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.cryptile.databinding.PromptLoadingBinding
import com.example.cryptile.databinding.PromptMessageBinding
import com.example.cryptile.databinding.PromptVerifyAccountBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AdditionalPrompts"

object AdditionalPrompts {
    private lateinit var loadingBinding: PromptLoadingBinding
    private lateinit var loadingDialog: Dialog

    /**
     * a simple yes or no prompt.
     * @param [title] the main title of the prompt.
     * @param [message] description of the action
     * @param [onSuccess] action to be performed if the user agrees
     */
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

    /**
     * @param [title] title of the loading prompt
     */
    fun initializeLoading(
        layoutInflater: LayoutInflater,
        context: Context,
        title: String
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            loadingBinding = PromptLoadingBinding.inflate(layoutInflater)
            loadingDialog = Dialog(context)
            loadingDialog.apply {
                setContentView(loadingBinding.root)
                window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
                setCancelable(false)
                show()
            }
            loadingBinding.progressTitleTextView.text = "$title..."
        }
    }

    /**
     * shows the given number as progress.
     * @param [progress] the value should be between 0-100.
     * @param [dismiss] some.
     */
    fun addProgress(
        progress: Int,
        dismiss: Boolean
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            if (dismiss) {
                loadingDialog.dismiss()
            } else {
                loadingBinding.progressBar.progress = progress
            }
        }
    }

    /**
     * uses email and password based verification if the user has an email based account
     * @param [notice] displays a notice of the action that will be performed once the user
     * enters the password and email
     * @param [onSuccess] task to be performed after the account has been verified
     */
    fun verifyUser(
        layoutInflater: LayoutInflater,
        context: Context,
        notice: String,
        usePassword: Boolean,
        onSuccess: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val email = auth.currentUser!!.email!!
        for (i in auth.currentUser!!.providerData) {
            Log.d(TAG, "providerData = ${i.providerId}")
            if ("google.com" == i.providerId && !usePassword) {
                // TODO: use fingerprint id
                Biometrics.verifyBiometrics(
                    context = context,
                    description = notice,
                    onSuccess = onSuccess,
                    onFailure = {}
                )
                return
            }
        }

        val binding = PromptVerifyAccountBinding.inflate(layoutInflater)
        val dialogBox = Dialog(context)
        dialogBox.apply {
            setContentView(binding.root)
            window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
            setCancelable(true)
            show()
        }

        binding.apply {
            noticeTextView.text = "*** $notice ***"
            confirmButton.setOnClickListener {
                val cred = EmailAuthProvider
                    .getCredential(email, userPasswordTextLayout.editText!!.text.toString())
                auth.currentUser!!.reauthenticate(cred).addOnSuccessListener {
                    Log.d(TAG, "user authenticated")
                    onSuccess()
                    dialogBox.dismiss()
                }.addOnFailureListener {
                    userPasswordTextLayout.apply {
                        error = "Password might be incorrect"
                        isErrorEnabled = true
                    }
                }
            }
            cancelButton.setOnClickListener { dialogBox.dismiss() }
        }
    }

    /**
     * displays a message as a prompt and performs an action after the dismiss button is pressed.
     * @param [message] message to be displayed on the screen
     * @param [onDismiss] task to be performed after the notice is dismissed.
     */
    fun showMessagePrompt(
        context: Context,
        layoutInflater: LayoutInflater,
        message: String,
        onDismiss: () -> Unit
    ) {
        val alertDialog = Dialog(context)
        val binding = PromptMessageBinding.inflate(layoutInflater)
        alertDialog.apply {
            setContentView(binding.root)
            window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
            setCancelable(false)
            show()
        }
        binding.apply {
            messageTextView.text = message
            dismissButton.setOnClickListener {
                alertDialog.dismiss()
                onDismiss()
            }
        }
    }
}