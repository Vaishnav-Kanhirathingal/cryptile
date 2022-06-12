package com.example.cryptile.ui_fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentMainBinding
import com.example.cryptile.databinding.PromptAddSafeBinding
import com.example.cryptile.databinding.PromptSignInBinding
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileReader


private const val TAG = "MainFragment"

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var menu: NavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topMenuBinding();mainBinding();sideBinding();getPermissions()
    }

    private fun topMenuBinding() {
        binding.includedSubLayout.topAppBar.setNavigationOnClickListener {
            binding.root.openDrawer(binding.navigationViewMainScreen)
        }
    }

    private fun mainBinding() {
        binding.includedSubLayout.addSafeFab.setOnClickListener {
            val promptAddSafeBinding = PromptAddSafeBinding.inflate(layoutInflater)
            val dialogBox = Dialog(requireContext())
            promptAddSafeBinding.apply {
                createSafe.setOnClickListener {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToCreateSafeFragment()
                    )
                    dialogBox.dismiss()
                }
                importSafe.setOnClickListener {
                    // TODO: open file explorer
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "text/plain"
                    startActivityForResult(intent, 1)
                    dialogBox.dismiss()
                }
            }
            dialogBox.apply {
                setContentView(promptAddSafeBinding.root)
                window!!.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setCancelable(true)
                show()
            }
        }
        // TODO: add adapter for safe recycler
        binding.includedSubLayout.safeRecycler
    }

    private fun sideBinding() {
        menu = binding.navigationViewMainScreen
        val headerMenu = menu.getHeaderView(0)

        headerMenu.apply {
            // TODO: change this to actual values
            findViewById<TextView>(R.id.name_text_view).text = "Some body"
            findViewById<TextView>(R.id.email_text_view).text = "Some Mail"
            findViewById<TextView>(R.id.phone_text_view).text = "Some Number"
            findViewById<TextView>(R.id.logged_in_text_view).text = "Logged In/Out"
        }
        menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.account_sign_in -> {
                    signInPrompt()
                    true
                }
                R.id.account_sign_out -> {
                    // TODO: prompt
                    true
                }
                R.id.account_sign_up -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSignUpFragment())
                    true
                }
                R.id.account_remove -> {
                    // TODO: prompt
                    true
                }
                R.id.safe_hide_all -> {
                    // TODO: prompt
                    true
                }
                R.id.settings -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
                    true
                }
                R.id.permission_manager -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToPermissionsFragment())
                    true
                }
                R.id.app_about -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToAboutFragment())
                    true
                }
                R.id.app_manual -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToDocumentationFragment())
                    true
                }
                R.id.app_exit -> {
                    // TODO: prompt
                    true
                }
                else -> {
                    throw IllegalArgumentException("menu item not set")
                }
            }
        }
    }

    //---------------------------------------------------------------------------------------prompts
    private fun signInPrompt() {
        val promptSignInBinding = PromptSignInBinding.inflate(layoutInflater)
        val dialogBox = Dialog(requireContext())
        promptSignInBinding.apply {
            cancelSignIn.setOnClickListener {
                Toast.makeText(requireContext(), "Sign In Failed", Toast.LENGTH_SHORT).show()
                dialogBox.dismiss()
            }
            signIn.setOnClickListener {
                // TODO: apply sign in bindings
            }
        }
        dialogBox.apply {
            setContentView(promptSignInBinding.root)
            window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
            show()
        }
    }

    //----------------------------------------------------------------------------permission-manager
    private fun getPermissions() {
        val permission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        Log.d(TAG, "requesting permissions")
        requireActivity().requestPermissions(permission, 100)
    }

    private fun readMetadata(path: String) {
        val reader = BufferedReader(FileReader(File(path)))
        var nextLine = reader.readLine()
        var fileDataString = ""
        while (!nextLine.isNullOrEmpty()) {
            fileDataString += "\n$nextLine"
            nextLine = reader.readLine()
        }
        Log.d(TAG, "string received = $fileDataString")
        val finalData = Gson().fromJson(fileDataString, SafeData::class.java)
        Log.d(TAG, finalData.toString())
        // TODO: get data from str and store to database
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val path =
                "/storage/emulated/0/${data!!.data!!.lastPathSegment!!.removePrefix("primary:")}"
            Log.d(TAG, "Safe Path = $path")
            if (path.isNullOrBlank() || !path.endsWith(".txt")) {
                Toast.makeText(
                    requireContext(), "Safe MetaData file not detected", Toast.LENGTH_SHORT
                ).show()
            } else {
                // TODO: get details from the metadata file.
                readMetadata(path)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "System Error, Reselect File", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}