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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentMainBinding
import com.example.cryptile.databinding.PromptAddSafeBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.adapters.SafeAdapter
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "MainFragment"

class MainFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var binding: FragmentMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    private val IMPORT_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        firebaseFirestore = Firebase.firestore
        mainBinding();sideMenuBinding();getPermissions();setValues()
    }

    private fun mainBinding() {
        binding.includedSubLayout.topAppBar.setNavigationOnClickListener {
            binding.root.openDrawer(binding.navigationViewMainScreen)
        }
        binding.includedSubLayout.addSafeFab.setOnClickListener {
            val promptAddSafeBinding = PromptAddSafeBinding.inflate(layoutInflater)
            val dialogBox = Dialog(requireContext())
            promptAddSafeBinding.apply {
                createSafeButton.setOnClickListener {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToCreateSafeFragment()
                    )
                    dialogBox.dismiss()
                }
                importSafeButton.setOnClickListener {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "text/plain"
                    startActivityForResult(intent, IMPORT_REQUEST_CODE)
                    dialogBox.dismiss()
                }
                cancelButton.setOnClickListener { dialogBox.dismiss() }
            }
            dialogBox.apply {
                setContentView(promptAddSafeBinding.root)
                window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setCancelable(true)
                show()
            }
        }
        val safeAdapter = SafeAdapter(
            viewModel = viewModel,
            lifeCycle = viewLifecycleOwner,
            inflater = layoutInflater,
            navController = findNavController(),
        )
        viewModel.getListOfIds().asLiveData()
            .observe(viewLifecycleOwner) { safeAdapter.submitList(it) }
        binding.includedSubLayout.safeRecycler.adapter = safeAdapter
        binding.includedSubLayout.safeRecycler
    }

    private fun sideMenuBinding() {
        val menu = binding.navigationViewMainScreen
        val headerMenu = menu.getHeaderView(0)

        headerMenu.apply {
            viewModel.userDisplayName.observe(viewLifecycleOwner) {
                findViewById<TextView>(R.id.name_text_view).text = it
            }
            viewModel.userEmail.observe(viewLifecycleOwner) {
                findViewById<TextView>(R.id.email_text_view).text = it
            }
        }
        menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.account_sign_in -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToSignInFragment()
                    )
                    true
                }
                R.id.account_sign_out -> {
                    // TODO: prompt
                    auth.signOut()
                    Toast.makeText(requireContext(), "Signed-Out", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToSignInFragment()
                    )
                    true
                }
                R.id.safe_remove_all -> {
                    // TODO: prompt
                    viewModel.deleteAll()
                    true
                }
                R.id.settings -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToSettingsFragment()
                    )
                    true
                }
                R.id.permission_manager -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToPermissionsFragment()
                    )
                    true
                }
                R.id.app_about -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToAboutFragment()
                    )
                    true
                }
                R.id.documentation -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToDocumentationFragment()
                    )
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

    private fun getPermissions() {
        val permission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        Log.d(TAG, "requesting permissions")
        requireActivity().requestPermissions(permission, 100)
    }

    private fun setValues() {
        firebaseFirestore
            .collection(UserDataConstants.tableName)
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener {
                viewModel.setData(
                    displayName = it.get(UserDataConstants.userDisplayName).toString(),
                    email = it.get(UserDataConstants.userEmail).toString(),
                    photoUrl = it.get(UserDataConstants.userPhotoUrl).toString(),
                )
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMPORT_REQUEST_CODE -> {
                try {
                    val path = data!!.data!!.lastPathSegment!!.removePrefix("primary:")
                    if (path.isBlank() || !path.endsWith(".txt")) {
                        Toast.makeText(
                            requireContext(), "Safe MetaData file not detected", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.insert(SafeData.load(path))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(), "System Error: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> Log.e(TAG, "request code didn't match")
        }
    }
}