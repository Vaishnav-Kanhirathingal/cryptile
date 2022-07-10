package com.example.cryptile.ui_fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.databinding.FragmentAboutBinding

private const val TAG = "AboutFragment"

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //code
        applyBinding()
    }

    private fun applyBinding() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            projectGithubButton.setOnClickListener { open("https://github.com/Vaishnav-Kanhirathingal/CRYPTILE") }
            personalWhatsappButton.setOnClickListener { open("https://wa.me/917219648837") }
            personalGithubButton.setOnClickListener { open("https://github.com/Vaishnav-Kanhirathingal") }
            personalGmailButton.setOnClickListener { open("mailto:vaishnav.kanhira@gmail.com") }
            personalOutlookButton.setOnClickListener { open("mailto:vaishnav.kanhira@outlook.com") }
            personalInstagramButton.setOnClickListener { open("https://www.instagram.com/vaishnav_k.p/") }
        }
    }

    private fun open(url: String) {
        Log.d(TAG, "opening url $url")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}