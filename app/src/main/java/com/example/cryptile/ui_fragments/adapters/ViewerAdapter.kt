package com.example.cryptile.ui_fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.R
import com.example.cryptile.data_classes.FileType
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.ListItemFileExplorerBinding

private const val TAG = "ViewerAdapter"

class ViewerAdapter(private val openFile: (SafeFiles) -> Unit) :
    ListAdapter<SafeFiles, ViewerAdapter.ViewerAdapterViewHolder>(diffCallBack) {

    class ViewerAdapterViewHolder(
        private val openFile: (SafeFiles) -> Unit,
        private val binding: ListItemFileExplorerBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(safeFiles: SafeFiles) {
            binding.apply {
                fileNameTextView.text = safeFiles.fileNameUpperCase
                fileDetailsTextView.text = "${safeFiles.fileAdded} | ${safeFiles.fileSize}"
                fileImageView.setImageResource(
                    when (safeFiles.fileType) {
                        FileType.UNKNOWN -> R.drawable.file_24
                        FileType.IMAGE -> R.drawable.image_24
                        FileType.VIDEO -> R.drawable.play_24
                        FileType.AUDIO -> R.drawable.audiotrack_24
                        FileType.DOCUMENT -> R.drawable.file_24
                        FileType.COMPRESSED -> R.drawable.archive_24
                        FileType.TEXT -> R.drawable.text_snippet_24
                    }
                )
            }
        }

        fun onClick(safeFiles: SafeFiles): Unit = openFile(safeFiles)
    }


    companion object {
        val diffCallBack = object : DiffUtil.ItemCallback<SafeFiles>() {
            override fun areItemsTheSame(oldItem: SafeFiles, newItem: SafeFiles) =
                oldItem.fileNameUpperCase == newItem.fileNameUpperCase

            override fun areContentsTheSame(oldItem: SafeFiles, newItem: SafeFiles) =
                ((oldItem.fileNameUpperCase == newItem.fileNameUpperCase) && (oldItem.fileAdded == newItem.fileAdded) && (oldItem.fileSize == newItem.fileSize))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewerAdapterViewHolder(
        openFile,
        ListItemFileExplorerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewerAdapterViewHolder, position: Int) {
        val x = getItem(position)
        holder.bind(x)
        holder.itemView.setOnClickListener { holder.onClick(x) }
    }
}