package com.example.cryptile.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.cryptile.app_data.room_files.SafeData.Companion.createRandomPartialKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SignInFunctions"

object SignInFunctions {
    suspend fun signInUsingGoogle(
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
                Log.d(
                    TAG, "user name - ${firebaseUser?.displayName}, " +
                            "email - ${firebaseUser?.email}"
                )
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
        email: String,
        password: String,
        auth: FirebaseAuth,
        context: Context,
        onSuccess: () -> Unit,
    ) {
        Log.d(TAG, "creating account using email")
        // TODO: open a prompt with loading screen
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "account creation successful")
                    if (auth.currentUser != null) {
                        auth.currentUser!!.sendEmailVerification().addOnCompleteListener { task ->
                            Log.d(TAG, "evaluating email")
                            if (task.isSuccessful) {
                                Log.d(TAG, "verification email sent")
                                Toast.makeText(
                                    context,
                                    "verification email sent, check inbox",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                // TODO: add values to database.
                            } else {
                                Log.d(
                                    TAG,
                                    "verification email generation failed, login and try again"
                                )
                                Toast.makeText(
                                    context,
                                    "verification email generation failed, login and try again",
                                    Toast.LENGTH_SHORT
                                ).show()
                                auth.signOut()
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "firebase error detected")
                it.printStackTrace()
                Toast.makeText(
                    context,
                    "registration failed try again later.\nReason - ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "new thread launched")
            Thread.sleep(40000)
            Log.d(TAG, "new thread continued after sleep")
            if (auth.currentUser!!.isEmailVerified) {
//                onSuccess()
                Log.d(TAG, "email verified finally 1234")
            } else {
                Log.d(TAG, "email not verified finally 1234")
            }
        }
        auth.addAuthStateListener {
        }
    }

    fun signInWithEmail(email: String, password: String, auth: FirebaseAuth, context: Context) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                if (auth.currentUser!!.isEmailVerified) {
                    Toast.makeText(context, "You have been logged in", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context, "Your email hasn't been verified yet", Toast.LENGTH_SHORT
                    ).show()
                    auth.currentUser!!.sendEmailVerification().addOnCompleteListener { task ->
                        CoroutineScope(Dispatchers.Main).launch {
                            if (task.isSuccessful) {
                                Toast.makeText(context, "verification complete", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                // TODO: logout
                                Toast.makeText(
                                    context,
                                    "verification incomplete, login and try again",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                auth.signOut()
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "error encountered: ${it.exception}")
                Toast.makeText(context, "exception: ${it.exception}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}