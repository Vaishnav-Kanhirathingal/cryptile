package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cryptile.databinding.FragmentCreateSafeBinding

class CreateSafeFragment : Fragment() {
    private lateinit var binding: FragmentCreateSafeBinding

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
        topBinding()
        mainBinding()
    }

    private fun topBinding() {
    }

    private fun mainBinding() {
    }
}