package com.example.cryptile.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.cryptile.data_classes.SafeFiles
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
                    UserDataConstants.userKey to SafeFiles.createRandomPartialKey(),// TODO: generate a unique key
                    UserDataConstants.userPhotoUrl to firebaseUser.photoUrl!!.toString()
                )
                database.collection(UserDataConstants.tableName).add(user)
                    .addOnSuccessListener {
                        Log.d(TAG, "adding to database successful")
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.d(TAG, "adding to database unsuccessful");e.printStackTrace()
                    }
                Toast.makeText(context, "Account Created", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "user was already registered")
                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
            }
            CoroutineScope(Dispatchers.Main).launch { actionOnSuccess() }
        }.addOnFailureListener {
            Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "addOnFailureListener")
            it.printStackTrace()
        }
    }
}