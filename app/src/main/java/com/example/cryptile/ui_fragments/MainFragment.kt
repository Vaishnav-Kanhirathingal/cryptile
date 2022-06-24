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
import com.example.cryptile.databinding.PromptSignInBinding
import com.example.cryptile.firebase.SignInFunctions
import com.example.cryptile.ui_fragments.adapters.SafeAdapter
import com.example.cryptile.view_models.AppViewModel
import com.example.cryptile.view_models.AppViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainFragment"

class MainFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity?.application as AppApplication).database.safeDao())
    }
    private lateinit var binding: FragmentMainBinding
    private lateinit var menu: NavigationView

    private lateinit var signInDialog: Dialog

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseFirestore: FirebaseFirestore

    private val IMPORT_REQUEST_CODE = 1
    private val GOOGLE_REQUEST_CODE = 2

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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        firebaseFirestore = Firebase.firestore

        mainBinding();sideBinding();getPermissions()
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
        // TODO: add adapter for safe recycler
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
                    auth.signOut()
                    Toast.makeText(requireContext(), "Signed-Out", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.account_sign_up -> {
                    findNavController().navigate(
                        MainFragmentDirections
                            .actionMainFragmentToSignUpFragment()
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
                        MainFragmentDirections
                            .actionMainFragmentToSettingsFragment()
                    )
                    true
                }
                R.id.permission_manager -> {
                    findNavController().navigate(
                        MainFragmentDirections
                            .actionMainFragmentToPermissionsFragment()
                    )
                    true
                }
                R.id.app_about -> {
                    findNavController().navigate(
                        MainFragmentDirections
                            .actionMainFragmentToAboutFragment()
                    )
                    true
                }
                R.id.app_manual -> {
                    findNavController().navigate(
                        MainFragmentDirections
                            .actionMainFragmentToDocumentationFragment()
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

    //---------------------------------------------------------------------------------------prompts
    private fun signInPrompt() {
        val promptSignInBinding = PromptSignInBinding.inflate(layoutInflater)
        signInDialog = Dialog(requireContext())
        promptSignInBinding.apply {
            cancelSignIn.setOnClickListener {
                Toast.makeText(requireContext(), "Sign In Failed", Toast.LENGTH_SHORT).show()
                signInDialog.dismiss()
            }
            signIn.setOnClickListener {
                // TODO: apply sign in bindings
            }
            googleSignInButton.setOnClickListener {
                startActivityForResult(googleSignInClient.signInIntent, GOOGLE_REQUEST_CODE)
            }
        }
        signInDialog.apply {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMPORT_REQUEST_CODE -> {
                try {
                    val path = data!!.data!!.lastPathSegment!!.removePrefix("primary:")
                    Log.d(TAG, "Safe Path = $path")
                    if (path.isNullOrBlank() || !path.endsWith(".txt")) {
                        Toast.makeText(
                            requireContext(), "Safe MetaData file not detected", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.insert(SafeData.load(path))
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
            GOOGLE_REQUEST_CODE -> {
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d(TAG, "google sign-in started")
                        SignInFunctions.signInUsingGoogle(
                            id = GoogleSignIn.getSignedInAccountFromIntent(data).result.idToken,
                            context = requireContext(),
                            auth = auth,
                            database = firebaseFirestore
                        ) {
                            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT)
                                .show()
                            signInDialog.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Log.e(TAG, "request code didn't match")
        }
    }
}