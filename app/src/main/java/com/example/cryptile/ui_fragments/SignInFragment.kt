package com.example.cryptile.ui_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.databinding.FragmentSignInBinding
import com.example.cryptile.firebase.SignInFunctions
import com.example.cryptile.firebase.UserDataConstants
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "SignInFragment"

class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseFirestore: FirebaseFirestore

    private val GOOGLE_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        firebaseFirestore = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBindings()
    }

    private fun applyBindings() {
        binding.apply {
            loginButton.setOnClickListener {
                val pass = userPasswordTextLayout.editText!!.text.toString()
                val passCorrect = pass.length in 7..33
                userPasswordTextLayout.error = "password isn't 8-32 characters long"
                userPasswordTextLayout.isErrorEnabled = !passCorrect

                val emailCorrect = userEmailTextLayout.editText!!.text.toString().isNotEmpty()
                userEmailTextLayout.error = "Email empty"
                userEmailTextLayout.isErrorEnabled = !emailCorrect

                if (passCorrect && emailCorrect) {
                    SignInFunctions.signInWithEmail(
                        email = userEmailTextLayout.editText!!.text.toString(),
                        password = pass,
                        auth = auth,
                        context = requireContext(),
                        layoutInflater = layoutInflater,
                        onSuccess = {
                            userEmailTextLayout.isErrorEnabled = false
                            Toast.makeText(
                                context,
                                "You have been logged in",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(
                                SignInFragmentDirections.actionSignInFragmentToMainFragment()
                            )
                        },
                        onFailure = {
                            Log.e(TAG, it)
                            userEmailTextLayout.isErrorEnabled = true
                            userEmailTextLayout.error = it
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Enter correct email and password values",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            googleSignUpButton.setOnClickListener {
                startActivityForResult(googleSignInClient.signInIntent, GOOGLE_REQUEST_CODE)
            }
            emailSignUpButton.setOnClickListener {
                findNavController().navigate(
                    SignInFragmentDirections.actionSignInFragmentToSignUpFragment()
                )
            }
            testButton.setOnClickListener {
                firebaseFirestore.collection(UserDataConstants.tableName).get()
                    .addOnSuccessListener {
                        for (results in it) {
                            Log.d(TAG, "data derived for [${results.id}] = [${results.data}]")
                        }
                    }.addOnFailureListener {
                        it.printStackTrace()
                    }
                Log.d(TAG,"uid= ${auth.currentUser!!.uid}")
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GOOGLE_REQUEST_CODE -> {
                try {
                    Log.d(TAG, "google sign-in started")
                    SignInFunctions.signInUsingGoogle(
                        id = GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken,
                        context = requireContext(),
                        auth = auth,
                        database = firebaseFirestore
                    ) {
                        Toast.makeText(
                            requireContext(),
                            "Google Login Successful",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        findNavController().navigate(
                            SignInFragmentDirections.actionSignInFragmentToMainFragment()
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Log.e(TAG, "request code didn't match")
        }
    }
}