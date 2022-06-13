package com.example.cryptile.ui_fragments.adapters

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.ListItemSafeBinding
import com.example.cryptile.databinding.PromptOpenSafeBinding
import com.example.cryptile.view_models.AppViewModel

private const val TAG = "SafeAdapter"

class SafeAdapter(
    private val viewModel: AppViewModel,
    private val lifeCycle: LifecycleOwner,
    private val inflater: LayoutInflater,
    private val context: Context
) :
    ListAdapter<Int, SafeAdapter.SafeAdapterViewHolder>(diffCallBack) {

    class SafeAdapterViewHolder(private val binding: ListItemSafeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(safeData: SafeData) {
            binding.apply {
                fileNameTextView.text = safeData.safeName
                safeTypeImageView.setImageResource(
                    if (safeData.personalAccessOnly) {
                        R.drawable.personal_safe_24
                    } else {
                        R.drawable.lock_24
                    }
                )
                safeOwnerTextView.text = safeData.safeOwner
                safeAbsolutePathTextView.text = safeData.safeAbsoluteLocation
                safeIsMultiPasswordTextView.text =
                    if (safeData.safeUsesMultiplePassword) "YES" else "no"
                safeIsPrivateTextView.text = if (safeData.personalAccessOnly) {
                    "private Safe"
                    // TODO: also add if current user has access
                } else {
                    "Public Safe"
                }
            }
        }

        fun promptListener(inflater: LayoutInflater, context: Context, safeData: SafeData) {
            val binding: PromptOpenSafeBinding = PromptOpenSafeBinding.inflate(inflater)
            val dialogBox = Dialog(context)
            binding.apply {
                // TODO: apply button bindings.
                binding.apply {
                    safeNameTextView.text = safeData.safeName
                    removeImageButton.setOnClickListener {
                        // TODO: remove safe from database, don't delete
                    }
                    deleteImageButton.setOnClickListener {
                        // TODO: check password/s and delete entire safe directory
                    }
                    cancelButton.setOnClickListener { dialogBox.dismiss() }
                    openButton.setOnClickListener {
                        // TODO: check password validity and open file explorer fragment, pass id to the fragment
                    }
                }
            }
            dialogBox.apply {
                setContentView(binding.root)
                window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setCancelable(true)
                show()
            }
        }
    }

    companion object {
        val diffCallBack = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeAdapterViewHolder =
        SafeAdapterViewHolder(ListItemSafeBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: SafeAdapterViewHolder, position: Int) {
        viewModel.getById(getItem(position)).asLiveData().observe(lifeCycle) {
            try {
                holder.bind(it)
                val data = it
                holder.itemView.setOnClickListener {
                    Log.d(TAG, "item selected = $data")
                    holder.promptListener(
                        inflater,
                        context,
                        data
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}