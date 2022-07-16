package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.databinding.FragmentSignUpBinding
import com.example.cryptile.firebase.SignInFunctions

private const val TAG = "SignUpFragment"

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                        userName = userName,
                        email = userEmail,
                        password = passOne,
                        context = requireContext(),
                        layoutInflater = layoutInflater,
                        onSuccess = { findNavController().navigateUp() },
                        onFailure = {
                            userEmailTextLayout.error = it
                            userEmailTextLayout.isErrorEnabled = true
                        },
                    )
                }
            }
            cancelLogin.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
}