package com.example.cryptile.ui_fragments

import android.content.Intent
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.IOException
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
                val usesMultiPasswords = useMultiplePasswordsSwitch.isChecked
                createSafeFiles(
                    safeName = safeNameInputLayout.editText!!.text.toString().ifEmpty {
                        "CRYPTILE_" + SimpleDateFormat("yyyy_MM_dd").format(System.currentTimeMillis())
                    },
                    safeOwner = "get from datastore",
                    usesMultiplePasswords = usesMultiPasswords,
                    partialKey = SafeFiles.createRandomPartialKey(),
                    personalAccessOnly = personalAccessOnlySwitch.isChecked,
                    encryptionAlgorithmUsed = when (encryptionLevelSlider.value) {
                        1.0f -> "one"
                        2.0f -> "two"
                        else -> "three"
                    }
                )
                //findNavController().navigate(CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment())
            }
        }
    }

    private fun createSafeFiles(
        safeName: String,
        safeOwner: String,
        usesMultiplePasswords: Boolean,
        partialKey: String,
        personalAccessOnly: Boolean,
        encryptionAlgorithmUsed: String
    ) {
        try {
            val safePath = "${currentPath.value}/$safeName"
            val fileDirectory = File(Environment.getExternalStorageDirectory(), safePath)
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs()
                /**
                 * making a directory is necessary since the 'saveChangesToMetadata'
                 * function used below doesn't create a directory.
                 */
            } else {
                // TODO: throw a warning toast and return
            }
            val safeData = SafeData(
                safeName = safeName,
                safeOwner = safeOwner,
                safeUsesMultiplePassword = usesMultiplePasswords,
                safePartialKey = partialKey,
                personalAccessOnly = personalAccessOnly,
                encryptionAlgorithm = encryptionAlgorithmUsed,
                safeCreated = System.currentTimeMillis(),
                safeAbsoluteLocation = safePath,
            )
            SafeFiles.saveChangesToMetadata(safeData)
            SafeFiles.saveChangesToLogFile(
                "\t\t\t\t-------------safe-created-------------\n", safePath, safeData.safeName
            )
            // TODO: set master key properly.
            SafeFiles.generateTestFilesAndStorageDirectory(safePath, "masterKey")
            viewModel.insert(safeData)
        } catch (e: IOException) {
            e.printStackTrace()
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