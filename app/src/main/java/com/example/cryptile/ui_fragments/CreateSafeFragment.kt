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
import com.example.cryptile.databinding.FragmentCreateSafeBinding
import java.io.File
import java.io.FileWriter
import java.io.IOException

private const val TAG = "CreateSafeFragment"

class CreateSafeFragment : Fragment() {
    private lateinit var binding: FragmentCreateSafeBinding
    private var location: MutableLiveData<String> = MutableLiveData("root/")
    private var useMultiplePasswords = MutableLiveData<Boolean>(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
            location.observe(viewLifecycleOwner) { currentSafeDirectory.text = it }
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
                    safeName = safeNameInputLayout.editText!!.text.toString(),
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
                File(Environment.getExternalStorageDirectory(), "Cryptile/$safeName")
            //file generated at above given location.
            if (!fileDirectory.exists()) {
                fileDirectory.mkdirs()
            }
            val filepath = File(fileDirectory, "SAFE_META_DATA_$safeName.txt")
            val writer = FileWriter(filepath)
            writer.append(
                "File Safe Details:-" +
                        "\n{" +
                        "\n\t" + "\"safeName\" : \"$safeName\"" +
                        "\n\t" + "\"safeOwner\" : \"$safeOwner\"" +
                        "\n\t" + "\"usesMultiplePasswords\" : \"$usesMultiplePasswords\"" +
                        "\n\t" + "\"ownerSignedPartialKeyOne\" : \"$ownerSignedPartialKeyOne\"" +
                        "\n\t" + "\"ownerSignedPartialKeyTwo\" : \"$ownerSignedPartialKeyTwo\"" +
                        "\n\t" + "\"personalAccessOnly\" : \"$personalAccessOnly\"" +
                        "\n\t" + "\"encryptionAlgorithmUsed\" : \"$encryptionAlgorithmUsed\"" +
                        "\n\t" + "\"time\": " + "\"${System.currentTimeMillis()}\"" +
                        "\n}"
                // TODO: add test values and their cipher for password verification
            )
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
            val path = data!!.data!!.path ?: "root"
            location.value = path
            Log.d(TAG, "data = ${location.value}")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "System Error, Reselect Path", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}