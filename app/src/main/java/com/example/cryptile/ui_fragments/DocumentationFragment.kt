package com.example.cryptile.ui_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cryptile.databinding.FragmentDocumentationBinding
import com.example.cryptile.ui_fragments.documentation.DocAdapter
import com.example.cryptile.ui_fragments.documentation.Docs

class DocumentationFragment : Fragment() {
    private lateinit var binding: FragmentDocumentationBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDocumentationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBinding()
    }

    private fun applyBinding() {
        binding.apply {
            topAppBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            val adapter = DocAdapter()
            docRecycler.adapter = adapter
            adapter.submitList(Docs.list)
        }
    }
}