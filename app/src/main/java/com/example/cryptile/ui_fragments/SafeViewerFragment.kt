package com.example.cryptile.ui_fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cryptile.R
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.FragmentSafeViewerBinding
import com.example.cryptile.databinding.PromptSafeSettingsBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.adapters.ViewerAdapter
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.example.cryptile.worker.CleanerWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

class SafeViewerFragment : Fragment() {
    val TAG: String = this::class.java.simpleName

    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private var safeDataId: Int? = null
    private lateinit var key: List<SecretKey>
    private lateinit var safeData: SafeData
    private lateinit var binding: FragmentSafeViewerBinding
    private lateinit var viewerAdapter: ViewerAdapter
    private lateinit var addFile: ActivityResultLauncher<Intent>
    private var fileList: MutableLiveData<List<SafeFiles>> = MutableLiveData(listOf())

    /**
     * here, the argument contains a list of keys as json strings. The list of keys are then
     * retrieved from the json string. This list is then used for encryption end decryption.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safeDataId = requireArguments().getInt("id")
        val list = Gson().fromJson(
            requireArguments().getString("key")!!,
            mutableListOf<String>()::class.java
        )
        val keyList = mutableListOf<SecretKey>()
        for (i in list) {
            keyList.add(SafeData.stringToKey(i))
        }
        key = keyList
        registerActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSafeViewerBinding.inflate(layoutInflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBindings()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyBindings() {
        binding.apply {
            topAppBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.add_file -> {
                        startAdditionActivity()
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
                        sendLog()
                        true
                    }

                    else -> false
                }
            }
            topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
            viewerAdapter = ViewerAdapter(
                opener = { safeFile: SafeFiles ->
                    CoroutineScope(Dispatchers.IO).launch {
                        safeData.openFile(
                            keyList = key,
                            safeFile = safeFile,
                            context = requireContext(),
                            layoutInflater = layoutInflater,
                            fileOpener = { file: File ->
                                val uri: Uri = getUriForFile(
                                    requireContext(),
                                    "com.example.cryptile.fileProvider",
                                    file
                                )
                                val mime = MimeTypeMap
                                    .getSingleton()
                                    .getMimeTypeFromExtension(safeFile.extension.substring(1))
                                Log.d(TAG, "mime = $mime")

                                val intent = Intent(Intent.ACTION_GET_CONTENT)
                                intent.action = Intent.ACTION_VIEW
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                intent.setDataAndType(uri, mime)
                                Log.d(
                                    TAG, "data received:" +
                                            "uri = $uri\n" +
                                            "extension = ${safeFile.extension.substring(1)}\n" +
                                            "path = ${file.absolutePath}\n" +
                                            "size = ${file.totalSpace} "
                                )
                                CoroutineScope(Dispatchers.Main).launch {
                                    requireContext().startActivity(intent)
                                }
                            }
                        )
                    }
                },
                exporter = {
                    CoroutineScope(Dispatchers.IO).launch {
                        safeData.export(it, key, requireContext(), layoutInflater)
                    }
                },
                deleter = { safeData.deleteFile(it);fileList.value = safeData.getDataFileList() },
                layoutInflater = layoutInflater
            )
            fileListRecyclerView.adapter = viewerAdapter
            viewModel.getById(safeDataId!!).asLiveData().observe(viewLifecycleOwner) {
                safeData = it
                fileList.value = it.getDataFileList()
                topAppBar.title = it.safeName

            }
            fileList.observe(viewLifecycleOwner) {
                viewerAdapter.submitList(it)
                emptyRecyclerTextView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }

            addFileBottomButton.setOnClickListener { startAdditionActivity() }
        }
    }

    /**
     * creates an intent to send the current safe's logs to the email account
     * vaishnav.kanhira@gmail.com for problem analysis.
     */
    private fun sendLog() {
        AdditionalPrompts.confirmationPrompt(
            context = requireContext(),
            title = "Send Log?",
            message = "Log files contain a list of actions that you have " +
                    "performed from the date of it's creation. Logs do not " +
                    "contain any personal information other than your accounts " +
                    "user name and the type of files you imported/opened/exported " +
                    "along with its size. (eg - extension - \'.mp4\' size - 32 MB). " +
                    "Log files can be useful for the developer to figure out any " +
                    "faults within the app. Continue?",
            onSuccess = {
                val emailIntent =
                    Intent(Intent.ACTION_SEND, Uri.parse("mailto:vaishnav.kanhira@gmail.com"))
                emailIntent.apply {
                    this.type = "text/plain"
                    this.putExtra(Intent.EXTRA_EMAIL, arrayOf("vaishnav.kanhira@gmail.com"))
                    this.putExtra(
                        Intent.EXTRA_SUBJECT,
                        "[CRYPTILE] - Sending log files for ${viewModel.userEmail.value}"
                    )
                    this.putExtra(
                        Intent.EXTRA_TEXT,
                        "[enter your issue associated with the safe below]"
                    )
                }
                val file = File(
                    Environment.getExternalStorageDirectory(),
                    "${safeData.safeAbsoluteLocation}/${SafeData.logFileName}"
                )
                if (!file.exists() || !file.canRead()) {
                    Toast.makeText(
                        requireContext(),
                        "Sending logs failed. You might have to do it manually",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val uri =
                        getUriForFile(requireContext(), "com.example.cryptile.fileProvider", file)
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"))
                }
            }
        )
    }

    private fun startAdditionActivity() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        addFile.launch(intent)
    }

    /**
     * a prompt with all the safe's settings wil be launched. here, the user can can change the
     * safe's name, password, delete and export the entire safe.
     */
    @RequiresApi(Build.VERSION_CODES.P)
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
            safeNameInputLayout.editText!!.setText(safeData.safeName)
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
                    onSuccess = {
                        CoroutineScope(Dispatchers.IO).launch {
                            safeData.exportAll(key, requireContext(), layoutInflater)
                        }
                    }
                )
            }
            deleteSafe.setOnClickListener {
                AdditionalPrompts.verifyUser(
                    layoutInflater = layoutInflater,
                    context = requireContext(),
                    notice = "delete safe?",
                    usePassword = false,
                    onSuccess = {
                        findNavController().navigateUp()
                        viewModel.delete(safeData)
                        safeData.deleteSafe()
                        dialogBox.dismiss()
                    }
                )
            }
            useMultiplePasswordsSwitch.setOnCheckedChangeListener { _, isChecked ->
                safePasswordTwoInputLayout.isEnabled = isChecked
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
                if (p1Check && p2Check) {
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
                                .get()
                                .addOnSuccessListener {
                                    val temp = safeData
                                    temp.safeUsesMultiplePassword =
                                        useMultiplePasswordsSwitch.isChecked
                                    temp.personalAccessOnly =
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
                                    dialogBox.dismiss()
                                    CoroutineScope(Dispatchers.IO).launch {
                                        safeData.changeEncryption(
                                            context = requireContext(),
                                            layoutInflater = layoutInflater,
                                            oldKey = key,
                                            newKey = newKeyList,
                                            onProcessFinish = {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Password Changed for safe: ${safeData.safeName}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                        safeData = temp
                                        viewModel.update(safeData)
                                        key = newKeyList
                                    }
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

    /**
     * registers activity to import a new file to the safe.
     */
    private fun registerActivity() {
        // TODO: fix issue for path
        addFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                val path = it.data!!.data!!.lastPathSegment!!.removePrefix("primary:")
                Log.d(TAG, "File Path = $path")
                if (path.isBlank()) {
                    Toast.makeText(requireContext(), "File not detected", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            safeData.importFileToSafe(
                                fileAbsolutePath = path,
                                keyList = key,
                                context = requireContext(),
                                layoutInflater = layoutInflater
                            )
                            CoroutineScope(Dispatchers.Main).launch {
                                fileList.value = safeData.getDataFileList()
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                e.printStackTrace()
                                Toast.makeText(
                                    requireContext(),
                                    "error importing file - ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "System Error, Reselect File",
                    Toast.LENGTH_SHORT
                )
                    .show()
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        Log.d(TAG, "clearing_cache started after onPause")
        val x = Gson().toJson(safeData)
        Log.d(TAG, "data sent is $x")
        val data = Data.Builder().putString(CleanerWorker.safeJsonKey, x).build()
        val cacheWorker =
            OneTimeWorkRequestBuilder<CleanerWorker>()
                .setInputData(data)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(requireContext()).beginUniqueWork(
            "clearing_cache",
            ExistingWorkPolicy.REPLACE,
            cacheWorker
        ).enqueue()
        super.onPause()
    }
}