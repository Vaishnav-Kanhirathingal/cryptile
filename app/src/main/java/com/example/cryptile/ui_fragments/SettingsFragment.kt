package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.databinding.FragmentSettingsBinding
import com.example.cryptile.firebase.FirebaseFunctions
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SettingsFragment"

class SettingsFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var dataStore: AppDataStore

    private lateinit var fireStore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStore = AppDataStore(requireContext())
        fireStore = Firebase.firestore
        auth = Firebase.auth
        applyBinding()
    }

    /**
     * the layout is divided into different sections. each sections bindings are done separately.
     */
    private fun applyBinding() {
        appSettingSectionBinding()
        userNameSectionBinding()
        changeAccountPasswordSectionBinding()
        deleteAccountSectionBinding()
    }

    private fun appSettingSectionBinding() {
        binding.apply {
            dataStore.keepMeSignedInFlow.asLiveData().observe(viewLifecycleOwner) {
                keepMeSignedInSwitch.isChecked = it
            }
            dataStore.fingerprintAppLockFlow.asLiveData().observe(viewLifecycleOwner) {
                useFingerprintSwitch.isChecked = it
            }
            topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
            keepMeSignedInSwitch.setOnCheckedChangeListener { _, isChecked ->
                CoroutineScope(Dispatchers.IO).launch {
                    dataStore.booleanSaver(isChecked, StoreBoolean.KEEP_ME_SIGNED_IN)
                }
            }
            useFingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
                CoroutineScope(Dispatchers.IO).launch {
                    dataStore.booleanSaver(isChecked, StoreBoolean.USER_USES_FINGERPRINT)
                }
            }
        }
    }

    private fun userNameSectionBinding() {
        binding.newUserNameTextLayout.apply {
            this.editText!!.setText(viewModel.userDisplayName.value)
            this.setEndIconOnClickListener {
                val newName = this.editText!!.text.toString()
                val lCheck = newName.length in 7..33
                Log.d(TAG, "lCheck = $lCheck")
                if (lCheck) {
                    AdditionalPrompts.verifyUser(
                        layoutInflater = layoutInflater,
                        context = requireContext(),
                        notice = "change user name",
                        usePassword = false,
                        onSuccess = {
                            fireStore
                                .collection(UserDataConstants.tableName)
                                .document(auth.uid!!)
                                .update(UserDataConstants.userDisplayName, newName)
                                .addOnSuccessListener {
                                    viewModel.updateDisplayName(newName)
                                    Toast.makeText(
                                        requireContext(),
                                        "display name changed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            Log.d(TAG, "user  name changed")
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "length should be 8-32 characters",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun changeAccountPasswordSectionBinding() {
        binding.apply {
            accountPasswordLayout.visibility =
                if (FirebaseFunctions.isAnEmailAccount()) View.VISIBLE else View.GONE
            forgotPasswordImageButton.setOnClickListener {
                AdditionalPrompts.confirmationPrompt(
                    context = requireContext(),
                    title = "Password Reset?",
                    message = "This action would send a password reset link in an email to your account. Clicking the link will reset your password and ask for a new password. Proceed?",
                    onSuccess = {
                        auth.sendPasswordResetEmail(auth.currentUser!!.email!!)
                            .addOnSuccessListener {
                                AdditionalPrompts.showMessagePrompt(
                                    context = requireContext(),
                                    layoutInflater = layoutInflater,
                                    message = getString(R.string.reset_password_message),
                                    onDismiss = {
                                        findNavController()
                                            .navigate(SettingsFragmentDirections.actionSettingsFragmentToSignInFragment())
                                    }
                                )
                            }.addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(
                                    requireContext(),
                                    "failed to generate a password reset email. error - ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                )
            }
            ClearPasswordImageButton.setOnClickListener {
                newPasswordTextLayout.editText!!.setText("")
                newPasswordRepeatTextLayout.editText!!.setText("")
            }
            confirmPasswordImageButton.setOnClickListener {
                val p1Check: Boolean
                val passwordOne: String
                newPasswordTextLayout.apply {
                    passwordOne = this.editText!!.text.toString()
                    p1Check = passwordOne.length in 7..33
                    this.error = "length should be 8-32 characters"
                    this.isErrorEnabled = !p1Check
                }
                val p2Check: Boolean
                newPasswordRepeatTextLayout.apply {
                    p2Check = passwordOne == this.editText!!.text.toString()
                    this.error = "Passwords don't match"
                    this.isErrorEnabled = !p2Check
                }
                if (p1Check && p2Check) {
                    AdditionalPrompts.verifyUser(
                        layoutInflater = layoutInflater,
                        context = requireContext(),
                        notice = "Change password",
                        usePassword = true,
                        onSuccess = {
                            Log.d(TAG, "password changed")
                            auth.currentUser!!.updatePassword(passwordOne).addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "password changed, login again",
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController()
                                    .navigate(SettingsFragmentDirections.actionSettingsFragmentToSignInFragment())
                            }.addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(
                                    requireContext(),
                                    "password change failed, error - ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        }
                    )
                }
            }
        }
    }

    private fun deleteAccountSectionBinding() {
        binding.apply {
            conditionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                deleteAccountButton.isEnabled = isChecked
            }
            deleteAccountButton.setOnClickListener {
                AdditionalPrompts.verifyUser(
                    layoutInflater = layoutInflater,
                    context = requireContext(),
                    notice = "Deletion of account",
                    usePassword = true,
                    onSuccess = {
                        fireStore
                            .collection(UserDataConstants.tableName)
                            .document(auth.currentUser!!.uid)
                            .delete()
                            .addOnSuccessListener {
                                auth.currentUser!!.delete().addOnSuccessListener {
                                    AdditionalPrompts.showMessagePrompt(
                                        context = requireContext(),
                                        layoutInflater = layoutInflater,
                                        message = "Your account has been deleted. Sign in using another account to access the app.",
                                        onDismiss = {
                                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToSignInFragment())
                                        }
                                    )
                                }.addOnFailureListener {
                                    it.printStackTrace()
                                    Toast.makeText(
                                        requireContext(),
                                        "failed to delete account. Reason - ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }.addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(
                                    requireContext(),
                                    "data deletion unsuccessful. Reason - ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                )
            }
        }
    }
}