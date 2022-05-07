package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.R
import com.example.cryptile.databinding.FragmentMainBinding
import com.google.android.material.navigation.NavigationView

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var menu: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //code
        topMenuBinding()
        mainBinding()
        sideBinding()
    }


    private fun topMenuBinding() {
        binding.includedSubLayout.topAppBar.setNavigationOnClickListener {
            binding.root.openDrawer(binding.navigationViewMainScreen)
        }
    }

    private fun mainBinding() {
        binding.includedSubLayout.addSafeFab.setOnClickListener {
            // TODO: a prompt to import or create a new safe
        }
        // TODO: add adapter for safe recycler
        binding.includedSubLayout.safeRecycler
    }

    private fun sideBinding() {
        menu = binding.navigationViewMainScreen
        val headerMenu = menu.getHeaderView(0)

        headerMenu.apply {
            findViewById<TextView>(R.id.name_text_view).text = "Some body"
            findViewById<TextView>(R.id.email_text_view).text = "Some Mail"
            findViewById<TextView>(R.id.phone_text_view).text = "Some Number"
        }
        menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.account_sign_in -> {
                    // TODO: prompt
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
                R.id.safe_settings -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
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
}