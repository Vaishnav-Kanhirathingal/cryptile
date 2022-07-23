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
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.app_data.data_store_files.AppDataStore
import com.example.cryptile.app_data.data_store_files.StoreBoolean
import com.example.cryptile.databinding.FragmentSignInBinding
import com.example.cryptile.firebase.FirebaseFunctions
import com.example.cryptile.ui_fragments.prompt.Biometrics
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val TAG = "SignInFragment"

open class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var googleSignInActivity: ActivityResultLauncher<Intent>

    private lateinit var dataStore: AppDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        firebaseFirestore = Firebase.firestore
        registerActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStore = AppDataStore(requireContext())
        applyBindings()
        navigateIfSignedIn()
    }

    private fun applyBindings() {
        binding.apply {
            loginButton.setOnClickListener {
                val pass = userPasswordTextLayout.editText!!.text.toString()
                val passCorrect = pass.length in 7..33
                userPasswordTextLayout.error = "password isn't 8-32 characters long"
                userPasswordTextLayout.isErrorEnabled = !passCorrect

                val emailCorrect = userEmailTextLayout.editText!!.text.toString().isNotEmpty()
                userEmailTextLayout.error = "Email empty"
                userEmailTextLayout.isErrorEnabled = !emailCorrect

                if (passCorrect && emailCorrect) {
                    FirebaseFunctions.signInWithEmail(
                        email = userEmailTextLayout.editText!!.text.toString(),
                        password = pass,
                        context = requireContext(),
                        layoutInflater = layoutInflater,
                        onSuccess = {
                            userEmailTextLayout.isErrorEnabled = false
                            Toast.makeText(
                                context,
                                "You have been logged in",
                                Toast.LENGTH_SHORT
                            ).show()
                            CoroutineScope(Dispatchers.IO).launch {
                                dataStore.booleanSaver(
                                    keepUserSignedInCheckbox.isChecked,
                                    StoreBoolean.KEEP_ME_SIGNED_IN,
                                )
                                dataStore.booleanSaver(
                                    binding.fingerprintLockSwitch.isChecked,
                                    StoreBoolean.USER_USES_FINGERPRINT
                                )
                            }
                            navigateToMain()
                        },
                        onFailure = {
                            Log.e(TAG, it)
                            userEmailTextLayout.isErrorEnabled = true
                            userEmailTextLayout.error = it
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Enter correct email and password values",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            googleSignUpButton.setOnClickListener { googleSignInActivity.launch(googleSignInClient.signInIntent) }
            emailSignUpButton.setOnClickListener {
                findNavController().navigate(SignInFragmentDirections.actionSignInFragmentToSignUpFragment())
            }
        }
    }

    /**
     * navigates to the main screen if conditions are correct
     */
    private fun navigateIfSignedIn() {
        val navigateUnderConditions = {
            if (auth.currentUser != null) {
                dataStore.keepMeSignedInFlow.asLiveData()
                    .observe(viewLifecycleOwner) {
                        if (it) {
                            navigateToMain()
                        }
                    }
            }
        }
        dataStore.fingerprintAppLockFlow.asLiveData().observe(viewLifecycleOwner) { useFinger ->
            if (useFinger) {
                Biometrics.verifyBiometrics(
                    context = requireContext(),
                    description = "Scan fingerprint to access app",
                    onSuccess = navigateUnderConditions,
                    onFailure = {
                        Log.d(TAG, "failed authentication")
                        auth.signOut()
//                        ActivityCompat.finishAffinity(requireActivity())
                        // TODO: exit app or log out
                    })
            } else navigateUnderConditions()

        }
    }

    /**
     * this is the navigation function to go to the main fragment. This is necessary to ensure that
     * the user is logged in.
     */
    private fun navigateToMain() {
        if (auth.currentUser != null) {
            findNavController().navigate(SignInFragmentDirections.actionSignInFragmentToMainFragment())
        }
    }

    private fun registerActivity() {
        googleSignInActivity =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    Log.d(TAG, "google sign-in started")
                    FirebaseFunctions.signInUsingGoogle(
                        id = GoogleSignIn.getSignedInAccountFromIntent(it.data).result.idToken,
                        context = requireContext(),
                        onSuccess = {
                            Toast.makeText(
                                requireContext(),
                                "Google Login Successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            CoroutineScope(Dispatchers.IO).launch {
                                dataStore.booleanSaver(true, StoreBoolean.KEEP_ME_SIGNED_IN)
                                dataStore.booleanSaver(
                                    binding.fingerprintLockSwitch.isChecked,
                                    StoreBoolean.USER_USES_FINGERPRINT
                                )
                            }
                            navigateToMain()
                        },
                        onFailure = { e: String ->
                            Toast.makeText(
                                requireContext(),
                                "an error has occurred: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}