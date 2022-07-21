package com.example.cryptile.ui_fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.cryptile.R
import com.example.cryptile.app_data.AppApplication
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.FragmentMainBinding
import com.example.cryptile.databinding.PromptAddSafeBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.adapters.SafeAdapter
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
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
import java.net.URL

private const val TAG = "MainFragment"

class MainFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var binding: FragmentMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var dataStore: AppDataStore
    private lateinit var importSafe: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStore = AppDataStore(requireContext())
        auth = Firebase.auth
        firebaseFirestore = Firebase.firestore
        mainBinding();sideMenuBinding();getPermissions();setViewModelValues()
    }

    private fun mainBinding() {
        binding.includedSubLayout.topAppBar.setNavigationOnClickListener {
            binding.root.openDrawer(binding.navigationViewMainScreen)
        }
        binding.includedSubLayout.addSafeFab.setOnClickListener { addSafePrompt() }
        val safeAdapter = SafeAdapter(
            viewModel = viewModel,
            lifeCycle = viewLifecycleOwner,
            inflater = layoutInflater,
            navController = findNavController(),
        )
        viewModel.getListOfIds().asLiveData().observe(viewLifecycleOwner) {
            safeAdapter.submitList(it)
            binding.includedSubLayout.emptySafeTextView.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
        }
        binding.includedSubLayout.safeRecycler.adapter = safeAdapter
        binding.includedSubLayout.safeRecycler.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun sideMenuBinding() {
        val menu = binding.navigationViewMainScreen
        val headerMenu = menu.getHeaderView(0)
        headerMenu.apply {
            CoroutineScope(Dispatchers.IO).launch {
                repeat(2) {
                    try {
                        Thread.sleep(3000)
                        Log.d(TAG, "url = ${viewModel.userPhotoUrl.value!!}")
                        val url = URL(viewModel.userPhotoUrl.value!!)
                        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                        CoroutineScope(Dispatchers.Main).launch {
                            findViewById<ImageView>(R.id.user_image).setImageBitmap(bmp)
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            findViewById<ImageView>(R.id.user_image).setImageResource(R.drawable.google)
                            e.printStackTrace()
                        }
                    }
                }
            }
            viewModel.userDisplayName.observe(viewLifecycleOwner) {
                findViewById<TextView>(R.id.name_text_view).text = it
            }
            viewModel.userEmail.observe(viewLifecycleOwner) {
                findViewById<TextView>(R.id.email_text_view).text = it
            }
        }
        menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.account_sign_out -> {
                    AdditionalPrompts.confirmationPrompt(
                        context = requireContext(),
                        title = "Sign Out?",
                        message = "Sign Out from your current signed in account [${viewModel.userDisplayName.value}]?",
                        onSuccess = {
                            CoroutineScope(Dispatchers.IO).launch {
                                dataStore.booleanSaver(false, StoreBoolean.KEEP_ME_SIGNED_IN)
                            }
                            auth.signOut()
                            Toast.makeText(requireContext(), "Signed-Out", Toast.LENGTH_SHORT)
                                .show()
                            findNavController().navigate(
                                MainFragmentDirections.actionMainFragmentToSignInFragment()
                            )
                        }
                    )
                    true
                }
                R.id.safe_remove_all -> {
                    AdditionalPrompts.confirmationPrompt(
                        context = requireContext(),
                        title = "Remove list?",
                        message = "Remove all CRYPTILE safes from the list (does not delete them)?",
                        onSuccess = { viewModel.deleteAll() }
                    )
                    true
                }
                R.id.settings -> {
                    findNavController().navigate(
                        MainFragmentDirections.actionMainFragmentToSettingsFragment()
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
                    AdditionalPrompts.confirmationPrompt(
                        context = requireContext(),
                        title = "Exit app?",
                        message = "Using this option also signs you out of your account " +
                                "regardless of whether you have enabled \'keep me signed in\'. Proceed?",
                        onSuccess = {
                            auth.signOut()
                            ActivityCompat.finishAffinity(requireActivity())
                        }
                    )
                    true
                }
                else -> {
                    throw IllegalArgumentException("menu item not set")
                }
            }
        }
    }

    private fun addSafePrompt() {
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
                AdditionalPrompts.confirmationPrompt(
                    context = requireContext(),
                    title = "Import Safe?",
                    message = "To import an already created safe into the app, navigate to " +
                            "the safe's folder and find the file named " +
                            "${SafeData.metaDataFileName}. Selecting this file would import " +
                            "the safe into the app. Continue?",
                    onSuccess = {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "text/plain"
                        importSafe.launch(intent)
                        dialogBox.dismiss()
                    }
                )
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

    private fun getPermissions() {
        val permission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        Log.d(TAG, "requesting permissions")
        requireActivity().requestPermissions(permission, 100)
    }

    /**
     * takes values from fire store and stores them in the view model
     */
    private fun setViewModelValues() {
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

    private fun registerActivity() {
        importSafe = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                val path = it.data!!.data!!.lastPathSegment!!.removePrefix("primary:")
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
    }
}