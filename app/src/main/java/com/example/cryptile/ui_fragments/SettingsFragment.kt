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
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.databinding.FragmentSettingsBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.google.firebase.auth.ktx.auth
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
        applyBinding()
    }

    private fun applyBinding() {
        binding.apply {
            newUserNameTextLayout.editText!!.setText(viewModel.userDisplayName.value)
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
            newUserNameTextLayout.apply {
                this.setEndIconOnClickListener {
                    val newName = this.editText!!.text.toString()
                    val lCheck = newName.length in 7..33
                    Log.d(TAG, "lCheck = $lCheck")
                    if (lCheck) {
                        AdditionalPrompts.verifyUser(
                            layoutInflater = layoutInflater,
                            context = requireContext(),
                            notice = "change user name",
                            onSuccess = {
                                Firebase.firestore
                                    .collection(UserDataConstants.tableName)
                                    .document(Firebase.auth.uid!!)
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
            forgotPasswordImageButton.setOnClickListener {
                // TODO: password reset email and message
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
                        onSuccess = {
                            Log.d(TAG, "password changed")
                            // TODO: use old and new password to change password
                        }
                    )
                }
            }
            conditionCheckBox.setOnCheckedChangeListener { _, isChecked ->
                deleteAccountButton.isEnabled = isChecked
            }
            deleteAccountButton.setOnClickListener {
                AdditionalPrompts.verifyUser(
                    layoutInflater = layoutInflater,
                    context = requireContext(),
                    notice = "Deletion of account",
                    onSuccess = {
                        // TODO: delete account
                    }
                )
            }
            // TODO: account settings for password reset, display name reset, delete for email
            // TODO: account settings for gmail as delete account
        }
    }
}