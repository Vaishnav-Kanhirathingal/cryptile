package com.example.cryptile.ui_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.databinding.FragmentCreateSafeBinding

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
    ): View? {
        binding = FragmentCreateSafeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainBinding()
    }

    private fun mainBinding() {
        //---------------------------------------------------------------------------top-bar-binding
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigate(CreateSafeFragmentDirections.actionCreateSafeFragmentToMainFragment())
        }
        //-----------------------------------------------------------------------multi-password-card
        binding.useMultiplePasswordsSwitch.setOnCheckedChangeListener { compoundButton, status ->
            useMultiplePasswords.value = status
        }
        useMultiplePasswords.observe(viewLifecycleOwner) {
            binding.safePasswordTwoInputLayout.isEnabled = it
        }
        //---------------------------------------------------------------------------------path-card
        binding.selectDirectoryImageButton.setOnClickListener {
            selectDirectory()
        }
        location.observe(viewLifecycleOwner) {
            binding.currentSafeDirectory.text = it
        }
    }

    private fun selectDirectory() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "System Error", Toast.LENGTH_SHORT).show()
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
        val x: String = ""
    }
}