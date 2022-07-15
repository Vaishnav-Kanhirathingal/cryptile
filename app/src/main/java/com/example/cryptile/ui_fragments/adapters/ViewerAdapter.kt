package com.example.cryptile.ui_fragments.adapters

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.R
import com.example.cryptile.data_classes.FileType
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.ListItemFileExplorerBinding
import com.example.cryptile.databinding.PromptFileOptionsBinding

private const val TAG = "ViewerAdapter"

class ViewerAdapter(
    private val opener: (SafeFiles) -> Unit,
    private val exporter: (SafeFiles) -> Unit,
    private val deleter: (SafeFiles) -> Unit,
    private val layoutInflater: LayoutInflater,
) : ListAdapter<SafeFiles, ViewerAdapter.ViewerAdapterViewHolder>(diffCallBack) {

    class ViewerAdapterViewHolder(
        private val opener: (SafeFiles) -> Unit,
        private val exporter: (SafeFiles) -> Unit,
        private val deleter: (SafeFiles) -> Unit,
        private val binding: ListItemFileExplorerBinding,
        private val context: Context,
        private val layoutInflater: LayoutInflater
    ) : RecyclerView.ViewHolder(binding.root) {
        /**
         * binds the UI side of the things
         */
        fun bind(safeFiles: SafeFiles) {
            binding.apply {
                fileNameTextView.text = safeFiles.fileNameUpperCase
                fileDetailsTextView.text =
                    "${safeFiles.fileAdded} | ${SafeFiles.getFormattedSize(safeFiles.fileSize)}"
                fileImageView.setImageResource(
                    when (safeFiles.fileType) {
                        FileType.UNKNOWN -> R.drawable.file_unknown_24
                        FileType.IMAGE -> R.drawable.image_24
                        FileType.VIDEO -> R.drawable.movie_24
                        FileType.AUDIO -> R.drawable.audio_24
                        FileType.DOCUMENT -> R.drawable.file_24
                        FileType.COMPRESSED -> R.drawable.compressed_24
                        FileType.TEXT -> R.drawable.text_file_24
                    }
                )
            }
        }

        fun onClick(safeFiles: SafeFiles): Unit = opener(safeFiles)

        fun onLongPressed(safeFiles: SafeFiles) {
            val dialog = Dialog(context)
            val binding = PromptFileOptionsBinding.inflate(layoutInflater)
            dialog.apply {
                setContentView(binding.root)
                window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setCancelable(true)
                show()
            }
            binding.apply {
                fileNameTextView.text = safeFiles.fileNameUpperCase + safeFiles.extension
                fileDetailsTextView.text =
                    "${safeFiles.fileAdded} | ${SafeFiles.getFormattedSize(safeFiles.fileSize)}"
                openButton.setOnClickListener { opener(safeFiles);dialog.dismiss() }
                exportButton.setOnClickListener { exporter(safeFiles);dialog.dismiss() }
                deleteButton.setOnClickListener { deleter(safeFiles);dialog.dismiss() }
            }
        }
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
        opener,
        exporter,
        deleter,
        ListItemFileExplorerBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        parent.context,
        layoutInflater
    )

    override fun onBindViewHolder(holder: ViewerAdapterViewHolder, position: Int) {
        val safeFile = getItem(position)
        holder.bind(safeFile)
        holder.itemView.setOnLongClickListener { holder.onLongPressed(safeFile);true }
        holder.itemView.setOnClickListener { holder.onClick(safeFile) }
    }
}