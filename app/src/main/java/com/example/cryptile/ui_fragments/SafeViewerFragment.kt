package com.example.cryptile.ui_fragments

import android.app.Dialog
import android.content.Intent
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
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentSafeViewerBinding
import com.example.cryptile.databinding.PromptSafeSettingsBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.adapters.ViewerAdapter
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

private const val TAG = "SafeViewerFragment"

class SafeViewerFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    var safeDataId: Int? = null
    private lateinit var key: List<SecretKey>
    private lateinit var safeData: SafeData
    private lateinit var binding: FragmentSafeViewerBinding
    private lateinit var viewerAdapter: ViewerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSafeViewerBinding.inflate(layoutInflater)
        return binding.root
    }

    /**
     * here, the argument contains a list of keys as json strings. The list of keys are then
     * retrieved from the json string. This list is then used for encryption end decryption.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safeDataId = arguments!!.getInt("id")
        val list = Gson().fromJson(
            arguments!!.getString("key")!!,
            mutableListOf<String>()::class.java
        )
        val keyList = mutableListOf<SecretKey>()
        for (i in list) {
            keyList.add(SafeData.stringToKey(i))
        }
        // TODO: get list keys from gson and assign
        key = keyList
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBindings()
    }

    private fun applyBindings() {
        binding.apply {
            topAppBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.add_file -> {
                        addFile()
                        true
                    }
                    R.id.safe_settings -> {
                        openSafeSettings()
                        true
                    }
                    R.id.clear_cache -> {
                        safeData.clearCache()
                        Toast.makeText(requireContext(), "cache cleared", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.send_log_files -> {
                        AdditionalPrompts.confirmationPrompt(
                            context = requireContext(),
                            title = "Send Log?",
                            message = "Log files contain a list of actions that you have " +
                                    "performed from the date of it's creation. Logs do not " +
                                    "contain any personal information other than your accounts " +
                                    "user name and the type of files you imported/opened/exported " +
                                    "along with its size. (eg - extension - \'.mp4\' size - 32 MB). " +
                                    "these files can be useful for the developer to figure out any " +
                                    "faults within the app. Continue?",
                            onSuccess = {
                                // TODO: open intent to send log file
                            }
                        )
                        true
                    }
                    else -> false
                }
            }
            topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
            viewerAdapter = ViewerAdapter(
                opener = { safeData.openFile(key, it, requireContext()) },
                exporter = { safeData.export(it, key) },
                deleter = { safeData.deleteFile(it);viewerAdapter.submitList(safeData.getDataFileList()) },
                layoutInflater = layoutInflater
            )
            fileListRecyclerView.adapter = viewerAdapter
            viewModel.getById(safeDataId!!).asLiveData().observe(viewLifecycleOwner) {
                safeData = it
                viewerAdapter.submitList(it.getDataFileList())
            }
            addFileBottomButton.setOnClickListener { addFile() }
        }
    }

    private fun addFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, 1)
    }

    private fun openSafeSettings() {
        val dialogBox = Dialog(requireContext())
        val settingsBinding = PromptSafeSettingsBinding.inflate(layoutInflater)
        dialogBox.apply {
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setContentView(settingsBinding.root)
            show()
            setCancelable(true)
        }
        settingsBinding.apply {
            safeNameInputLayout.setEndIconOnClickListener {
                val name = safeNameInputLayout.editText!!.text.toString()
                if (name.length in 7..32) {
                    AdditionalPrompts.confirmationPrompt(
                        context = requireContext(),
                        title = "Change Safe's name?",
                        message = "This action will change the display name of the " +
                                "safe but, the directory name will remain the same. Continue?",
                        onSuccess = {
                            safeData.safeName = name
                            safeData.saveChangesToMetadata()
                            viewModel.update(safeData)
                            Toast.makeText(
                                requireContext(),
                                "Safe name updated successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Safe name should be between 8-32 character long",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            exportAll.setOnClickListener {
                AdditionalPrompts.confirmationPrompt(
                    context = requireContext(),
                    title = "Export all files?",
                    message = "This action would decrypt all files to the export folder " +
                            "and then, remove all contents from the data folder rendering" +
                            " the safe empty. Continue?",
                    onSuccess = { safeData.exportAll(key) }
                )
            }
            cancelButton.setOnClickListener { dialogBox.dismiss() }
            applyButton.setOnClickListener {
                val p1Check: Boolean
                safePasswordOneInputLayout.apply {
                    p1Check = this.editText!!.text.toString().length in 7..33
                    this.error = "Password length should be 8-32 characters"
                    this.isErrorEnabled = !p1Check
                }
                val p2Check: Boolean
                safePasswordTwoInputLayout.apply {
                    if (useMultiplePasswordsSwitch.isChecked) {
                        p2Check = this.editText!!.text.toString().length in 7..33
                        this.error = "Password length should be 8-32 characters"
                        this.isErrorEnabled = !p2Check
                    } else {
                        p2Check = true
                    }
                }
                // TODO: replace with p1check && p2check
                if (true) {
                    AdditionalPrompts.confirmationPrompt(
                        context = requireContext(),
                        title = "Change security settings?",
                        message = "This action would re-encrypt all files in the safe to match " +
                                "with the newly generated keys. This action can take some time " +
                                "proportional to the size of the safe. Continue?",
                        onSuccess = {
                            Firebase
                                .firestore
                                .collection(UserDataConstants.tableName)
                                .document(Firebase.auth.currentUser!!.uid)
                                .get().addOnSuccessListener {
                                    safeData.safeUsesMultiplePassword =
                                        useMultiplePasswordsSwitch.isChecked
                                    safeData.personalAccessOnly =
                                        personalAccessOnlySwitch.isChecked
                                    val newKeyList =
                                        if (useMultiplePasswordsSwitch.isChecked) {
                                            safeData.getKey(
                                                safePasswordOneInputLayout.editText!!.text.toString(),
                                                safePasswordTwoInputLayout.editText!!.text.toString()
                                            )
                                        } else {
                                            safeData.getKey(safePasswordOneInputLayout.editText!!.text.toString())
                                        }
                                    if (personalAccessOnlySwitch.isChecked) {
                                        newKeyList.add(
                                            SafeData.stringToKey(
                                                it.get(UserDataConstants.userKey).toString()
                                            )
                                        )
                                    }
                                    safeData.changeEncryption(
                                        oldKey = key,
                                        newKey = newKeyList
                                    )
                                    viewModel.update(safeData)
                                    key = newKeyList
                                }.addOnFailureListener {
                                    it.printStackTrace()
                                    Toast.makeText(
                                        requireContext(),
                                        "an error has occurred: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val path = data!!.data!!.lastPathSegment!!.removePrefix("primary:")
            Log.d(TAG, "File Path = $path")
            if (path.isBlank()) {
                Toast.makeText(requireContext(), "File not detected", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    safeData.importFileToSafe(fileAbsolutePath = path, safeMasterKey = key)
                    CoroutineScope(Dispatchers.Main).launch {
                        viewerAdapter.submitList(safeData.getDataFileList())
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "System Error, Reselect File", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}