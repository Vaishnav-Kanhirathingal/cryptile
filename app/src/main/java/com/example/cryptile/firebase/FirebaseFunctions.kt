package com.example.cryptile.firebase

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData.Companion.createRandomKey
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SignInFunctions"

object SignInFunctions {
    /**
     * this function is used to complete google sign in or sign up process.
     * @param [id] takes the user id for the siged in account
     * @param [onSuccess] Lambda to run after task is a Success
     * @param [onFailure] Lambda to run after task is a Failure
     */
    fun signInUsingGoogle(
        id: String?,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val auth = Firebase.auth
        Log.d(TAG, "google sign-in function started")
        val credential = GoogleAuthProvider.getCredential(id, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                if (it.additionalUserInfo!!.isNewUser) {
                    Log.d(TAG, "user is new")
                    val firebaseUser = auth.currentUser
                    addToDatabase(
                        userName = firebaseUser!!.displayName!!,
                        email = firebaseUser.email!!,
                        photoURL = firebaseUser.photoUrl!!.toString(),
                        uid = firebaseUser.uid,
                        onFailure = onFailure
                    )
                    Toast.makeText(context, "Account Created", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "user was already registered")
                    Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                }
                CoroutineScope(Dispatchers.Main).launch { onSuccess() }
            }.addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "addOnFailureListener")
            }
    }

    /**
     * function to sign up the user using the data they provided.
     * @param [userName] typed in user name
     * @param [email] registered email
     * @param [password] current password
     * @param [onSuccess] this is a lambda that has the coe to run after the account has been
     * created and the user has been notified
     * @param [onFailure] code to run if the sign up fails
     */
    fun signUpWithEmail(
        userName: String,
        email: String,
        password: String,
        context: Context,
        layoutInflater: LayoutInflater,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = Firebase.auth
        Log.d(TAG, "creating account using email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "account creation successful")
                if (it.additionalUserInfo!!.isNewUser) {
                    Log.d(TAG, "user is new")
                    addToDatabase(
                        userName = userName,
                        email = email,
                        photoURL = "some photo url",
                        uid = auth.uid!!,
                        onFailure = onFailure
                    )
                    Toast.makeText(context, "Account Created", Toast.LENGTH_SHORT).show()
                }
                auth.currentUser!!.sendEmailVerification()
                    .addOnSuccessListener {
                        Log.d(TAG, "verification email sent")
                        auth.signOut()
                        AdditionalPrompts.showMessagePrompt(
                            context = context,
                            layoutInflater = layoutInflater,
                            message = context.resources.getString(R.string.email_sent_message),
                            onDismiss = onSuccess
                        )
                    }.addOnFailureListener { e: Exception ->
                        e.printStackTrace()
                        onFailure(e.message!!)
                        auth.signOut()
                        Toast.makeText(
                            context,
                            "Verification email generation failed, try again later. Reason: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }.addOnFailureListener {
                it.printStackTrace()
                onFailure(it.message!!)
                Toast.makeText(
                    context,
                    "Account generation failed. Reason: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * saves value to the database
     */
    private fun addToDatabase(
        userName: String,
        email: String,
        photoURL: String,
        uid: String,
        onFailure: (String) -> Unit,
    ) {
        val user = hashMapOf(
            UserDataConstants.userDisplayName to userName,
            UserDataConstants.userEmail to email,
            UserDataConstants.userKey to createRandomKey(),
            UserDataConstants.userPhotoUrl to photoURL
        )
        Log.d(TAG, "uid = $uid")
        val database = Firebase.firestore
        database
            .collection(UserDataConstants.tableName)
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "adding to database successful")
            }
            .addOnFailureListener {
                Log.d(TAG, "adding to database unsuccessful")
                it.printStackTrace()
                onFailure(it.message!!)
            }
    }

    /**
     * @param [onSuccess] task to perform on Success
     * @param [onFailure] task to perform on Failure, takes error message string as parameter
     */
    fun signInWithEmail(
        email: String,
        password: String,
        context: Context,
        layoutInflater: LayoutInflater,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val auth = Firebase.auth
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (auth.currentUser!!.isEmailVerified) {
                    Log.d(TAG, "current user is email verified")
                    onSuccess()
                } else {
                    Log.d(TAG, "current user is not email verified")
                    auth.currentUser!!.sendEmailVerification()
                        .addOnSuccessListener {
                            AdditionalPrompts.showMessagePrompt(
                                context = context,
                                layoutInflater = layoutInflater,
                                message = context.resources.getString(R.string.email_sent_message),
                                onDismiss = {}
                            )
                        }.addOnFailureListener {
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
}