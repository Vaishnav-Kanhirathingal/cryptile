package com.example.cryptile.ui_fragments.documentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.databinding.ListItemDocumentationBinding

class DocAdapter : ListAdapter<Documentation, DocAdapter.DocAdapterViewHolder>(diffCallback) {

    class DocAdapterViewHolder(val binding: ListItemDocumentationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(documentation: Documentation) {
            binding.titleTextView.text = documentation.title
            binding.messageTextView.text = documentation.description
        }
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Documentation>() {
            override fun areItemsTheSame(oldItem: Documentation, newItem: Documentation): Boolean =
                oldItem.title == newItem.title

            override fun areContentsTheSame(
                oldItem: Documentation, newItem: Documentation
            ): Boolean = oldItem.description == newItem.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocAdapterViewHolder =
        DocAdapterViewHolder(
            ListItemDocumentationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: DocAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}