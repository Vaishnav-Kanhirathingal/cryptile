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
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentCreateSafeBinding
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat

private const val TAG = "CreateSafeFragment"

class CreateSafeFragment : Fragment() {
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
            /**
             * top app bar binding
             */
            topAppBar.setNavigationOnClickListener {
                findNavController().navigate(CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment())
            }
            /**
             * multi password card binding
             */
            useMultiplePasswordsSwitch
                .setOnCheckedChangeListener { _, status -> useMultiplePasswords.value = status }
            useMultiplePasswords.observe(viewLifecycleOwner) {
                safePasswordTwoInputLayout.isEnabled = it
            }
            /**
             * select directory card binding
             */
            selectDirectoryImageButton.setOnClickListener { selectDirectory() }
            currentPath.observe(viewLifecycleOwner) { currentSafeDirectory.text = it }
            /**
             * bottom confirmation and cancellation button bindings
             */
            cancelButton.setOnClickListener {
                findNavController().navigate(
                    CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment()
                )
            }
            confirmButton.setOnClickListener {
                // TODO: change provided values for test function.
                val usesMultiPasswords = useMultiplePasswordsSwitch.isChecked
                createSafeFiles(
                    safeName = if (safeNameInputLayout.editText!!.text.toString().isEmpty()) {
                        "CRYPTILE_SAFE_" + SimpleDateFormat("yyyy_MM_dd_hh:mm:ss_a").format(System.currentTimeMillis())
                    } else {
                        safeNameInputLayout.editText!!.text.toString()
                    },
                    safeOwner = "get from datastore",
                    usesMultiplePasswords = usesMultiPasswords,
                    ownerSignedPartialKeyOne = "ownerSignedPartialKeyOne",
                    ownerSignedPartialKeyTwo = (if (usesMultiPasswords) "ownerSignedPartialKeyTwo" else null),
                    personalAccessOnly = personalAccessOnlySwitch.isChecked,
                    encryptionAlgorithmUsed = when (encryptionLevelSlider.value) {
                        1.0f -> "one"
                        2.0f -> "two"
                        else -> "three"
                    }
                )
            }
        }
    }

    private fun createSafeFiles(
        safeName: String,
        safeOwner: String,
        usesMultiplePasswords: Boolean,
        ownerSignedPartialKeyOne: String,
        ownerSignedPartialKeyTwo: String?,
        personalAccessOnly: Boolean,
        encryptionAlgorithmUsed: String
    ) {
        try {
            val fileDirectory =
                File(Environment.getExternalStorageDirectory(), "${currentPath.value}/$safeName")
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs()
            }
            val filepath = File(fileDirectory, "SAFE_META_DATA.txt")
            val writer = FileWriter(filepath)
            val jsonMetadata = GsonBuilder().setPrettyPrinting().create().toJson(
                SafeData(
                    safeName = safeName,
                    safeOwner = safeOwner,
                    safeUsesMultiplePassword = usesMultiplePasswords,
                    safePartialPasswordOne = ownerSignedPartialKeyOne,
                    safePartialPasswordTwo = ownerSignedPartialKeyTwo,
                    personalAccessOnly = personalAccessOnly,
                    encryptionAlgorithm = encryptionAlgorithmUsed,
                    safeCreated = System.currentTimeMillis(),
                    testPlain = "plain text",
                    testCipher = "cipher text",
                    safeAbsoluteLocation = "remaining",
                )
            )
            writer.append(jsonMetadata)
            writer.flush()
            writer.close()
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