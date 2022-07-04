package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.databinding.FragmentSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var dataStore: AppDataStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
            newUserNameTextLayout.setEndIconOnClickListener {
                // TODO: change display name
            }
            confirmPasswordImageButton.setOnClickListener {
                // TODO: check old password
                val p1Check: Boolean
                val passwordOne: String
                newPasswordTextLayout.apply {
                    passwordOne = this.editText!!.text.toString()
                    p1Check = passwordOne.length in 7..33
                    this.error = "Password length should be 8-32 characters"
                    this.isErrorEnabled = !p1Check
                }
                val p2Check: Boolean
                newPasswordRepeatTextLayout.apply {
                    p2Check = passwordOne == this.editText!!.text.toString()
                    this.error = "Passwords don't match"
                    this.isErrorEnabled = !p2Check
                }
                if (p1Check && p2Check) {
                    // TODO: use old and new password to change password
                }
            }
            // TODO: account settings for password reset, display name reset, delete for email
            // TODO: account settings for gmail as delete account
        }
    }
}