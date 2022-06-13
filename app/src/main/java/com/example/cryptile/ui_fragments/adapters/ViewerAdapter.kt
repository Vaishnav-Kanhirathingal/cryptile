package com.example.cryptile.ui_fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.ListItemFileExplorerBinding

class ViewerAdapter :
    ListAdapter<SafeFiles, ViewerAdapter.ViewerAdapterViewHolder>(diffCallBack) {

    class ViewerAdapterViewHolder(private val binding: ListItemFileExplorerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(safeFiles: SafeFiles) {
            binding.apply {
                fileNameTextView.text = safeFiles.fileName
                fileDateTextView.text = safeFiles.fileCreated
                fileSizeTextView.text = safeFiles.fileSize
            }
        }

        fun onClick() {
            // TODO: extract data using key and open in respective app
        }
    }


    companion object {
        val diffCallBack = object : DiffUtil.ItemCallback<SafeFiles>() {
            override fun areItemsTheSame(oldItem: SafeFiles, newItem: SafeFiles) =
                oldItem.fileName == newItem.fileName

            override fun areContentsTheSame(oldItem: SafeFiles, newItem: SafeFiles) =
                ((oldItem.fileName == newItem.fileName) && (oldItem.fileCreated == newItem.fileCreated) && (oldItem.fileSize == newItem.fileSize))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewerAdapterViewHolder(
        ListItemFileExplorerBinding.inflate(LayoutInflater.from(parent.context))
    )

    override fun onBindViewHolder(holder: ViewerAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener { holder.onClick() }
    }
}