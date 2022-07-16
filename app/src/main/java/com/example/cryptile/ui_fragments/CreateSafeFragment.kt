package com.example.cryptile.ui_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentCreateSafeBinding
import com.example.cryptile.firebase.UserDataConstants
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
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import javax.crypto.SecretKey

private const val TAG = "CreateSafeFragment"

class CreateSafeFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    private lateinit var binding: FragmentCreateSafeBinding
    private lateinit var getPath: ActivityResultLauncher<Intent>

    private var currentPath: MutableLiveData<String> = MutableLiveData("Cryptile")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivity()
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
        auth = Firebase.auth
        firebaseFirestore = Firebase.firestore
        mainBinding()
    }

    private fun mainBinding() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            useMultiplePasswordsSwitch.setOnCheckedChangeListener { _, status ->
                safePasswordTwoInputLayout.isEnabled = status
            }
            selectDirectoryImageButton.setOnClickListener { selectDirectory() }
            currentPath.observe(viewLifecycleOwner) { currentSafeDirectory.text = it }
            cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
            confirmButton.setOnClickListener {
                val passwordCheck = verifyPasswordParameters()
                // TODO: replace with [passwordCheck]
                if (true) {
                    firebaseFirestore
                        .collection(UserDataConstants.tableName)
                        .document(auth.currentUser!!.uid)
                        .get()
                        .addOnSuccessListener {
                            createSafe(
                                passwordOne = safePasswordOneInputLayout.editText!!.text.toString(),
                                passwordTwo = safePasswordTwoInputLayout.editText!!.text.toString(),
                                personalKey = SafeData.stringToKey(
                                    it.get(UserDataConstants.userKey).toString()
                                ),
                            )
                        }.addOnFailureListener {
                            it.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                "safe creation error has occurred: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }
    }

    /**
     * verifies password length in the text boxes. it also checks is the safe uses multiple
     * passwords. It is also responsible for showing error on the password text layouts.
     */
    private fun verifyPasswordParameters(): Boolean {
        binding.apply {
            val p1Check: Boolean
            safePasswordOneInputLayout.apply {
                p1Check = this.editText!!.text.toString().length in 7..33
                this.error = "Password length should be 8-32 characters"
                this.isErrorEnabled = !p1Check
            }
            val p2Check: Boolean
            safePasswordTwoInputLayout.apply {
                p2Check = if (useMultiplePasswordsSwitch.isChecked) {
                    this.editText!!.text.toString().length in 7..33
                } else {
                    true
                }
                this.error = "Password length should be 8-32 characters"
                this.isErrorEnabled = !p2Check
            }
            return (p1Check && p2Check)
        }
    }

    /**
     * attempts to create a safe using the password/s and personal key. Only uses these values if
     * required. Responsible for creating directories, adding to database and navigating back if
     * the action was a success or giving a warning if it wasn't.
     */
    private fun createSafe(
        passwordOne: String,
        passwordTwo: String,
        personalKey: SecretKey,
    ) {
        binding.apply {
            val salt = ByteArray(32)
            SecureRandom().nextBytes(salt)
            val safeData = SafeData(
                safeName = safeNameInputLayout.editText!!.text.toString().ifEmpty {
                    "CRYPTILE_" + SimpleDateFormat("yyyy_MM_dd").format(System.currentTimeMillis())
                },
                safeOwner = viewModel.userDisplayName.value.toString(),
                safeUsesMultiplePassword = useMultiplePasswordsSwitch.isChecked,
                personalAccessOnly = personalAccessOnlySwitch.isChecked,
                encryptionAlgorithm = when (encryptionLevelSlider.value) {
                    1.0f -> "one"
                    2.0f -> "two"
                    else -> "three"
                },
                safeCreated = System.currentTimeMillis(),
                hideSafePath = pathHiddenSwitch.isChecked,
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
            if (personalAccessOnlySwitch.isChecked) {
                keyList.add(personalKey)
            }
            CoroutineScope(Dispatchers.IO).launch {
                safeData.generateDirectories(
                    masterKey = keyList,
                    onSuccess = {
                        CoroutineScope(Dispatchers.IO).launch { viewModel.insert(safeData) }
                        safeData.saveChangesToMetadata()

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                requireContext(),
                                "Files generated Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                    },
                    onFailure = {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                requireContext(),
                                "Encountered errors while creation of safe, a " +
                                        "folder the same name as the safe name already " +
                                        "exists at the designated location",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e(
                                TAG, "File generation for safe failed, directory " +
                                        "${safeData.safeAbsoluteLocation} already exists"
                            )
                        }
                    }
                )
            }
        }
    }

    private fun selectDirectory() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            getPath.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerActivity() {
        getPath = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                currentPath.value = it.data!!.data!!.lastPathSegment!!.removePrefix("primary:")
                Log.d(TAG, "data = ${currentPath.value}")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "System Error, Reselect Path", Toast.LENGTH_SHORT)
                    .show()
                e.printStackTrace()
            }
        }
    }
}