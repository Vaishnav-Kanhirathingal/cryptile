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
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.app_data.data_store_files.StoreString
import com.example.cryptile.databinding.FragmentSignUpBinding
import com.example.cryptile.firebase.SignInFunctions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SignUpFragment"

class SignUpFragment : Fragment() {
    private lateinit var dataStore: AppDataStore
    private lateinit var binding: FragmentSignUpBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseFirestore: FirebaseFirestore
    private val GOOGLE_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStore = AppDataStore(requireContext())

        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        firebaseFirestore = Firebase.firestore

        mainBinding()
    }

    private fun mainBinding() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController()
                    .navigate(SignUpFragmentDirections.actionSignUpFragmentToMainFragment())
            }
            googleSignUpButton.setOnClickListener {
                startActivityForResult(googleSignInClient.signInIntent, GOOGLE_REQUEST_CODE)
            }
            signUpButton.setOnClickListener {
                var userName: String
                userNameTextLayout.apply {
                    userName = this.editText!!.text.toString()
                    this.error = "Account Name length should be 8-32 characters"
                    this.isErrorEnabled = userName.length !in 7..33
                }
                val userEmail: String = userEmailTextLayout.editText!!.text.toString()
                val userUsesFingerprint: Boolean = userUseFingerprint.isChecked
                val passOne = userSetPasswordTextLayout.editText!!.text.toString()
                val passTwo = userRepeatPasswordTextLayout.editText!!.text.toString()

                if (passOne.length !in 7..33) {
                    userSetPasswordTextLayout.error = "Password length should be 8-32 characters"
                    userSetPasswordTextLayout.isErrorEnabled = true
                } else {
                    userSetPasswordTextLayout.isErrorEnabled = false
                    if (passOne == passTwo && userName.length in 7..33) {
                        SignInFunctions.signUpWithEmail(
                            email = userEmailTextLayout.editText!!.text.toString(),
                            password = userSetPasswordTextLayout.editText!!.text.toString(),
                            auth = auth,
                            context = requireContext(),
                            layoutInflater = layoutInflater,
                        ) {
                            CoroutineScope(Dispatchers.IO).launch {
                                dataStore.apply {
                                    stringSaver(userName, StoreString.USER_NAME)
                                    stringSaver(userEmail, StoreString.USER_EMAIL)
                                    booleanSaver(
                                        userUsesFingerprint,
                                        StoreBoolean.USER_USES_FINGERPRINT
                                    )
                                    booleanSaver(true, StoreBoolean.USER_LOGGED_IN)
                                }
                            }
                            Toast.makeText(
                                requireContext(), "verification complete", Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(
                                SignUpFragmentDirections.actionSignUpFragmentToMainFragment()
                            )
                        }
                    }
                }
            }
            cancelLogin.setOnClickListener {
                findNavController()
                    .navigate(
                        SignUpFragmentDirections
                            .actionSignUpFragmentToMainFragment()
                    )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_REQUEST_CODE) {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "google sign-in started")
                    SignInFunctions.signInUsingGoogle(
                        id = GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken,
                        context = requireContext(),
                        auth = auth,
                        database = firebaseFirestore
                    ) {
                        findNavController()
                            .navigate(
                                SignUpFragmentDirections
                                    .actionSignUpFragmentToMainFragment()
                            )
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
            }
        } else Log.e(TAG, "request code didn't match")
    }
}