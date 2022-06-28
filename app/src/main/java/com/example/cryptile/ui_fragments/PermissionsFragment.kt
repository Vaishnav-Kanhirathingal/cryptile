package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.databinding.FragmentPermissionsBinding

class PermissionsFragment : Fragment() {
    private lateinit var binding: FragmentPermissionsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPermissionsBinding.inflate(layoutInflater)
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
        }
    }
}