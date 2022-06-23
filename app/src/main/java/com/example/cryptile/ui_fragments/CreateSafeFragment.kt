package com.example.cryptile.ui_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.FragmentCreateSafeBinding
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat

private const val TAG = "CreateSafeFragment"

class CreateSafeFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var binding: FragmentCreateSafeBinding
    private var currentPath: MutableLiveData<String> =
        MutableLiveData("Cryptile")
    private var useMultiplePasswords = MutableLiveData(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateSafeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding()
    }

    private fun mainBinding() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController().navigate(CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment())
            }
            useMultiplePasswordsSwitch
                .setOnCheckedChangeListener { _, status -> useMultiplePasswords.value = status }
            useMultiplePasswords
                .observe(viewLifecycleOwner) { safePasswordTwoInputLayout.isEnabled = it }
            selectDirectoryImageButton.setOnClickListener { selectDirectory() }
            currentPath.observe(viewLifecycleOwner) { currentSafeDirectory.text = it }
            cancelButton.setOnClickListener {
                findNavController().navigate(
                    CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment()
                )
            }
            confirmButton.setOnClickListener {
                // TODO: change provided values for test function.

                val p1Check = safePasswordOneInputLayout.editText!!.text.toString().length > 7
                val p2Check = safePasswordTwoInputLayout.editText!!.text.toString().length > 7
                val conditionCheck = (p1Check && p2Check)
                // TODO: replace with condition chech
                if (true) {
                    val x = createSafe(
                        safePasswordOneInputLayout.editText!!.text.toString(),
                        safePasswordTwoInputLayout.editText!!.text.toString()
                    )
                    if (x) {
                        Toast.makeText(
                            requireContext(), "Files generated Successfully", Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment())
                    } else {
                        Toast.makeText(
                            requireContext(), "Encountered errors while creation of safe, a " +
                                    "folder the same name as the safe name already exists at the" +
                                    " designated location",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    safePasswordOneInputLayout.isErrorEnabled = p1Check
                    safePasswordTwoInputLayout.isErrorEnabled = p2Check
                    "password length should be between 8 and 32".apply {
                        safePasswordOneInputLayout.error = this
                        safePasswordTwoInputLayout.error = this
                    }
                }
            }
        }
    }

    private fun createSafe(
        passwordOne: String,
        passwordTwo: String
    ): Boolean {
        binding.apply {
            val salt = ByteArray(32)
            SecureRandom().nextBytes(salt)
            val safeData = SafeData(
                safeName = safeNameInputLayout.editText!!.text.toString().ifEmpty {
                    "CRYPTILE_" + SimpleDateFormat("yyyy_MM_dd").format(System.currentTimeMillis())
                },
                safeOwner = "get from data store",// TODO: get from data store
                safeUsesMultiplePassword = useMultiplePasswordsSwitch.isChecked,
                personalAccessOnly = personalAccessOnlySwitch.isChecked,
                encryptionAlgorithm = when (encryptionLevelSlider.value) {
                    1.0f -> "one"
                    2.0f -> "two"
                    else -> "three"
                },
                safeCreated = System.currentTimeMillis(),
                hideSafePath = !pathHiddenSwitch.isChecked,// TODO: change
                safeAbsoluteLocation = currentPath.value + "/" +
                        safeNameInputLayout.editText!!.text.toString().ifEmpty {
                            "CRYPTILE_" + SimpleDateFormat("yyyy_MM_dd").format(System.currentTimeMillis())

                        },
                safeSalt = String(salt, StandardCharsets.ISO_8859_1)
            )
            val keyList =
                if (safeData.safeUsesMultiplePassword) {
                    safeData.getKey(
                        passwordOne = passwordOne,
                        passwordTwo = passwordTwo,
                    )
                } else {
                    safeData.getKey(
                        passwordOne = passwordOne
                    )
                }
            // TODO: add personal key if necessary
            val fileGenerationStatus = safeData.generateDirectories(keyList)
            safeData.saveChangesToMetadata()
            safeData.saveChangesToLogFile("\t\t\t\t-------------safe-created-------------\n")
            if (fileGenerationStatus) viewModel.insert(safeData)
            return fileGenerationStatus
        }
    }

    private fun selectDirectory() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            currentPath.value = data!!.data!!.lastPathSegment!!.removePrefix("primary:")
            Log.d(TAG, "data = ${currentPath.value}")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "System Error, Reselect Path", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}