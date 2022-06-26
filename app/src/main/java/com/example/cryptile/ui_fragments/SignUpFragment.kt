package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.data_store_files.AppDataStore
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

private const val TAG = "SignUpFragment"

class SignUpFragment : Fragment() {
    private lateinit var dataStore: AppDataStore
    private lateinit var binding: FragmentSignUpBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseFirestore: FirebaseFirestore

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
            signUpButton.setOnClickListener {
                val userNameCorrect: Boolean
                var userName: String
                userNameTextLayout.apply {
                    userName = this.editText!!.text.toString()
                    userNameCorrect = userName.length in 7..33
                    this.error = "Account Name length should be 8-32 characters"
                    this.isErrorEnabled = !userNameCorrect
                }
                val userEmailCorrect: Boolean
                val userEmail: String
                userEmailTextLayout.apply {
                    userEmail = this.editText!!.text.toString()
                    userEmailCorrect = userEmail.isNotEmpty()
                    this.error = "email cannot be empty"
                    this.isErrorEnabled = !userEmailCorrect
                }

                val passOne = userSetPasswordTextLayout.editText!!.text.toString()
                val passTwo = userRepeatPasswordTextLayout.editText!!.text.toString()

                val passwordCorrect =
                    if (passOne.length !in 7..33) {
                        userSetPasswordTextLayout.error =
                            "Password length should be 8-32 characters"
                        userSetPasswordTextLayout.isErrorEnabled = true
                        false
                    } else if (passOne != passTwo) {
                        userSetPasswordTextLayout.error = "Passwords don't match"
                        userSetPasswordTextLayout.isErrorEnabled = true
                        false
                    } else {
                        userSetPasswordTextLayout.isErrorEnabled = false
                        true
                    }


                if (userNameCorrect && userEmailCorrect && passwordCorrect) {
                    SignInFunctions.signUpWithEmail(
                        email = userEmail,
                        password = passOne,
                        auth = auth,
                        context = requireContext(),
                        layoutInflater = layoutInflater,
                        onMessageDismiss = {
                            findNavController().navigate(
                                SignUpFragmentDirections.actionSignUpFragmentToSignInFragment()
                            )
                        }, onFailure ={
                            userEmailTextLayout.error = it
                            userEmailTextLayout.isErrorEnabled = true
                        }
                    )
                }
            }
            cancelLogin.setOnClickListener {
                findNavController().navigate(
                    SignUpFragmentDirections.actionSignUpFragmentToSignInFragment()
                )
            }
        }
    }
}