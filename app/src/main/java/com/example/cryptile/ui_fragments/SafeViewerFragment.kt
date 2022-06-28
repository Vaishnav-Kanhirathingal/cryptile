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
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.FragmentSafeViewerBinding
import com.example.cryptile.ui_fragments.adapters.ViewerAdapter
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
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
    var id: Int? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = arguments!!.getInt("id")
        val gson = arguments!!.getString("key")!!
        val list = Gson().fromJson(gson, mutableListOf<String>()::class.java)
        Log.d(TAG, "list recieved = $gson")
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
                        // TODO: start an intent to share logs to email
                        true
                    }
                    else -> false
                }
            }
            topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
            val opener: (SafeFiles) -> Unit = { safeData.openFile(key, it) }
            viewerAdapter = ViewerAdapter(opener)
            fileListRecyclerView.adapter = viewerAdapter
            viewModel.getById(id!!).asLiveData().observe(viewLifecycleOwner) {
                safeData = it
                viewerAdapter.submitList(it.getDataFileList())
            }
            addFileBottomButton.setOnClickListener { addFile() }
        }
    }

    private fun addFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/plain"
        startActivityForResult(intent, 1)
    }

    private fun openSafeSettings() {
        // TODO: open a settings prompt
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val path = data!!.data!!.lastPathSegment!!.removePrefix("primary:")
            Log.d(TAG, "File Path = $path")
            if (path.isBlank()) {
                Toast.makeText(requireContext(), "File not detected", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: if false, file already exists.
                CoroutineScope(Dispatchers.IO).launch {
                    safeData.importFileToSafe(fileAbsolutePath = path, safeMasterKey = key)
                    val list = safeData.getDataFileList()
                    CoroutineScope(Dispatchers.Main).launch { viewerAdapter.submitList(list) }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "System Error, Reselect File", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}