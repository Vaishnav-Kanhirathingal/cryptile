package com.example.cryptile.firebase

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData.Companion.createRandomPartialKey
import com.example.cryptile.databinding.PromptMessageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SignInFunctions"

object SignInFunctions {
    fun signInUsingGoogle(
        id: String?,
        context: Context,
        auth: FirebaseAuth,
        database: FirebaseFirestore,
        actionOnSuccess: () -> Unit
    ) {
        Log.d(TAG, "google sign-in function started")
        val credential = GoogleAuthProvider.getCredential(id, null)
        auth.signInWithCredential(credential).addOnSuccessListener {
            if (it.additionalUserInfo!!.isNewUser) {
                Log.d(TAG, "user is new")
                val firebaseUser = auth.currentUser
                val user = hashMapOf(
                    UserDataConstants.userDisplayName to firebaseUser!!.displayName!!,
                    UserDataConstants.userEmail to firebaseUser.email!!,
                    UserDataConstants.userKey to createRandomPartialKey(),
                    UserDataConstants.userPhotoUrl to firebaseUser.photoUrl!!.toString()
                )
                database.collection(UserDataConstants.tableName).add(user)
                    .addOnSuccessListener {
                        Log.d(TAG, "adding to database successful")
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.d(TAG, "adding to database unsuccessful")
                        e.printStackTrace()
                    }
                Toast.makeText(context, "Account Created", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "user was already registered")
                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
            }
            CoroutineScope(Dispatchers.Main).launch { actionOnSuccess() }
        }.addOnFailureListener {
            it.printStackTrace()
            Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "addOnFailureListener")
        }
    }

    fun signUpWithEmail(
        userName: String,
        email: String,
        password: String,
        auth: FirebaseAuth,
        database: FirebaseFirestore,
        context: Context,
        layoutInflater: LayoutInflater,
        onMessageDismiss: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(TAG, "creating account using email")
        // TODO: open a prompt with loading screen
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            Log.d(TAG, "account creation successful")
            // TODO: add values to database.
            if (it.additionalUserInfo!!.isNewUser) {
                Log.d(TAG, "user is new")
                val user = hashMapOf(
                    UserDataConstants.userDisplayName to userName,
                    UserDataConstants.userEmail to email,
                    UserDataConstants.userKey to createRandomPartialKey(),
                    UserDataConstants.userPhotoUrl to "some photo url"// TODO: set photo url correctly
                )
                database.collection(UserDataConstants.tableName).add(user)
                    .addOnSuccessListener {
                        Log.d(TAG, "adding to database successful")
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.d(TAG, "adding to database unsuccessful")
                        e.printStackTrace()
                    }
                Toast.makeText(context, "Account Created", Toast.LENGTH_SHORT).show()
            }


            if (auth.currentUser != null) {
                auth.currentUser!!.sendEmailVerification().addOnSuccessListener {
                    Log.d(TAG, "verification email sent")
                    auth.signOut()
                    showEmailSentPrompt(context, layoutInflater, onMessageDismiss)
                }.addOnFailureListener {
                    it.printStackTrace()
                    onFailure(it.message!!)
                    auth.signOut()
                    Toast.makeText(
                        context,
                        "Verification email generation failed, try again later. Reason: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.addOnFailureListener {
            // TODO: warning about failing to create user
            it.printStackTrace()
            onFailure(it.message!!)
            Toast.makeText(
                context,
                "Account generation failed. Reason: ${it.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        auth: FirebaseAuth,
        context: Context,
        layoutInflater: LayoutInflater,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (auth.currentUser!!.isEmailVerified) {
                    Log.d(TAG, "current user is email verified")
                    onSuccess()
                } else {
                    Log.d(TAG, "current user is not email verified")
                    auth.currentUser!!.sendEmailVerification()
                        .addOnSuccessListener {
                            showEmailSentPrompt(context, layoutInflater) {}
                        }.addOnFailureListener {
                            Log.e(TAG, "firebase error")
                            it.printStackTrace()
                            Log.d(TAG, "verification link generation failed. Reason: ${it.message}")
                            onFailure(it.message!!)
                            auth.signOut()
                        }
                }
            }.addOnFailureListener {
                it.printStackTrace()
                onFailure(it.message!!)
                Toast.makeText(context, "exception: $it", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmailSentPrompt(
        context: Context,
        layoutInflater: LayoutInflater,
        onDismiss: () -> Unit
    ) {
        val alertDialog = Dialog(context)
        val binding = PromptMessageBinding.inflate(layoutInflater)
        alertDialog.apply {
            setContentView(binding.root)
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
            show()
        }
        binding.apply {
            messageTextView.setText(R.string.email_sent_message)
            dismissButton.setOnClickListener {
                alertDialog.dismiss()
                onDismiss()
            }
        }
    }
}