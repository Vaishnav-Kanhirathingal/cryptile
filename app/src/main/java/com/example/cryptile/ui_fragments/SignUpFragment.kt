package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.app_data.data_store_files.StoreString
import com.example.cryptile.databinding.FragmentSignUpBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SignUpFragment"
class SignUpFragment : Fragment() {
    private lateinit var dataStore: AppDataStore
    private lateinit var binding: FragmentSignUpBinding
    private var phoneVerified = false
    private var emailVerified = false
    private var accountNameVerified = false

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
        mainBinding()
    }

    private fun mainBinding() {
        phoneCardBinding()
        emailCardBinding()
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToMainFragment())
            }
            signUpButton.setOnClickListener {
                var userName: String
                var userEmail: String
                var userPhoneNumber: String
                userNameTextLayout.apply {
                    userName = this.editText!!.text.toString()
                    accountNameVerified = userName.length in 7..33
                    this.error = "Account Name length should be 8-32 characters"
                    this.isErrorEnabled = !accountNameVerified
                }
                userEmailTextLayout.apply {
                    userEmail = this.editText!!.text.toString()
                    if (emailVerified) {
                        this.isErrorEnabled = false
                    } else {
                        this.isErrorEnabled = true
                        this.error = "Email not set"
                    }
                }
                userPhoneNumberTextLayout.apply {
                    userPhoneNumber = this.editText!!.text.toString()
                    if (phoneVerified) {
                        this.isErrorEnabled = false
                    } else {
                        this.isErrorEnabled = true
                        this.error = "Phone number not set"
                    }
                }
                val userUsesFingerprint: Boolean = userUseFingerprint.isChecked
                val passOne = userSetPasswordTextLayout.editText!!.text.toString()
                val passTwo = userRepeatPasswordTextLayout.editText!!.text.toString()

                if (passOne.length !in 7..33) {
                    userSetPasswordTextLayout.error = "Password length should be 8-32 characters"
                    userSetPasswordTextLayout.isErrorEnabled = true
                } else {
                    userSetPasswordTextLayout.isErrorEnabled = false
                    if (passOne == passTwo && phoneVerified && emailVerified && accountNameVerified) {
                        // TODO: store values
                        CoroutineScope(Dispatchers.IO).launch {
                            dataStore.apply {
                                stringSaver(userName, StoreString.USER_NAME)
                                stringSaver(userEmail, StoreString.USER_EMAIL)
                                stringSaver(userPhoneNumber, StoreString.USER_PHONE)
                                booleanSaver(
                                    userUsesFingerprint,
                                    StoreBoolean.USER_USES_FINGERPRINT
                                )
                                booleanSaver(true, StoreBoolean.USER_LOGGED_IN)
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToMainFragment())
                            }
                        }
                    }
                }
            }
            cancelLogin.setOnClickListener {
                findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToMainFragment())
            }
        }
    }


    private fun phoneCardBinding() {
        binding.apply {
            var phoneOTP: String = "0"
            userPhoneNumberTextLayout.apply {
                this.setEndIconOnClickListener {
                    this.isEnabled = false
                    phoneOTP = "1111"// TODO: send a request to generate otp and receive otp
                }
            }
            verifyPhoneOtp.apply {
                this.setOnClickListener {
                    val otp = userPhoneOtpTextLayout.editText!!.text.toString()
                    if (otp == phoneOTP && otp.length == 4) {
                        phoneVerified = true
                        this.isEnabled = false
                        userPhoneNumberTextLayout.isEnabled = false
                        userPhoneNumberTextLayout.isErrorEnabled = false
                        userPhoneOtpTextLayout.isEnabled = false
                        userPhoneOtpTextLayout.isErrorEnabled = false
                    } else {
                        userPhoneOtpTextLayout.error = "OTP does not match"
                        userPhoneOtpTextLayout.isErrorEnabled = true
                    }

                }
            }
            clearPhoneOtp.setOnClickListener {
                phoneOTP = "0"
                phoneVerified = false
                userPhoneNumberTextLayout.apply {
                    this.editText!!.setText("")
                    this.isEnabled = true
                }
                userPhoneOtpTextLayout.apply {
                    this.editText!!.setText("")
                    this.isEnabled = true
                    this.isErrorEnabled = false
                    verifyPhoneOtp.isEnabled = true
                }
            }
        }
    }

    private fun emailCardBinding() {
        binding.apply {
            var emailOTP = "0"
            userEmailTextLayout.apply {
                this.setEndIconOnClickListener {
                    this.isEnabled = false
                    emailOTP = "1111"// TODO: send a request to generate otp and receive otp
                }
            }
            verifyEmailOtp.apply {
                this.setOnClickListener {
                    val otp =
                        userEmailOtpTextLayout.editText!!.text.toString()
                    if (otp == emailOTP && otp.length == 4) {
                        emailVerified=true
                        this.isEnabled = false
                        userEmailTextLayout.isEnabled = false
                        userEmailTextLayout.isErrorEnabled = false
                        userEmailOtpTextLayout.isEnabled = false
                        userEmailOtpTextLayout.isErrorEnabled = false
                    } else {
                        userEmailOtpTextLayout.error = "OTP does not match"
                        userEmailOtpTextLayout.isErrorEnabled = true
                    }

                }
            }
            clearEmailOtp.setOnClickListener {
                emailOTP = "0"
                emailVerified = false
                userEmailTextLayout.apply {
                    this.editText!!.setText("")
                    this.isEnabled = true
                }
                userEmailOtpTextLayout.apply {
                    this.editText!!.setText("")
                    this.isEnabled = true
                    this.isErrorEnabled = false
                    verifyPhoneOtp.isEnabled = true
                }
            }
        }
    }
}