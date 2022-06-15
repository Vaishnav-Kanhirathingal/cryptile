package com.example.cryptile.ui_fragments.adapters

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.data_classes.SafeFiles
import com.example.cryptile.databinding.ListItemSafeBinding
import com.example.cryptile.databinding.PromptOpenSafeBinding
import com.example.cryptile.ui_fragments.MainFragmentDirections
import com.example.cryptile.view_models.AppViewModel

private const val TAG = "SafeAdapter"

class SafeAdapter(
    private val viewModel: AppViewModel,
    private val lifeCycle: LifecycleOwner,
    private val inflater: LayoutInflater,
    private val navController: NavController,
) :
    ListAdapter<Int, SafeAdapter.SafeAdapterViewHolder>(diffCallBack) {

    class SafeAdapterViewHolder(
        private val binding: ListItemSafeBinding,
        private val context: Context
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(safeData: SafeData) {
            binding.apply {
                fileNameTextView.text = safeData.safeName
                safeTypeImageView.setImageResource(if (safeData.personalAccessOnly) R.drawable.personal_safe_24 else R.drawable.lock_24)
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

        fun promptListener(
            inflater: LayoutInflater,
            safeData: SafeData,
            viewModel: AppViewModel,
            navController: NavController
        ) {
            val binding: PromptOpenSafeBinding = PromptOpenSafeBinding.inflate(inflater)
            val dialogBox = Dialog(context)
            binding.apply {
                safeNameTextView.text = safeData.safeName
                passwordTwoTextLayout.isEnabled = safeData.safeUsesMultiplePassword
                resetImageButton.setOnClickListener {
                    passwordOneTextLayout.isEnabled = true
                    passwordTwoTextLayout.isEnabled = safeData.safeUsesMultiplePassword
                }
                removeImageButton.setOnClickListener { viewModel.delete(safeData);dialogBox.dismiss() }
                deleteImageButton.setOnClickListener {
                    // TODO: check password/s and delete entire safe directory if personal safe is disabled
                    dialogBox.dismiss()
                }
                passwordOneTextLayout.setEndIconOnClickListener {
                    passwordOneTextLayout.isEnabled = false
                }
                passwordTwoTextLayout.setEndIconOnClickListener {
                    passwordTwoTextLayout.isEnabled = false
                }
                cancelButton.setOnClickListener { dialogBox.dismiss() }
                openButton.setOnClickListener {
                    val key = if (safeData.safeUsesMultiplePassword) {
                        SafeFiles.getKey(
                            passwordOneTextLayout.editText!!.text.toString(),
                            safeData.safePartialPasswordOne,
                            safeData.personalAccessOnly,
                            passwordTwoTextLayout.editText!!.text.toString(),
                            safeData.safePartialPasswordTwo!!
                        )
                    } else {
                        SafeFiles.getKey(
                            passwordOneTextLayout.editText!!.text.toString(),
                            safeData.safePartialPasswordOne,
                            safeData.personalAccessOnly
                        )
                    }
                    val keyIsCorrect: Boolean =
                        SafeFiles.checkKeyGenerated(key, safeData.safeAbsoluteLocation)
                    if (keyIsCorrect) {
                        navController.navigate(
                            MainFragmentDirections
                                .actionMainFragmentToSafeViewerFragment(safeData.id, key)
                        )
                    } else {
                        passwordOneTextLayout.apply {
                            error = "Password might be incorrect"; isErrorEnabled = true
                        }
                        passwordTwoTextLayout.apply {
                            error = "Password might be incorrect"; isErrorEnabled = true
                        }
                    }
                    dialogBox.dismiss()
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
        SafeAdapterViewHolder(
            ListItemSafeBinding.inflate(LayoutInflater.from(parent.context)), parent.context
        )

    override fun onBindViewHolder(holder: SafeAdapterViewHolder, position: Int) {
        viewModel.getById(getItem(position)).asLiveData().observe(lifeCycle) {
            try {
                holder.bind(it)
                val data = it
                holder.itemView.setOnClickListener {
                    Log.d(TAG, "item selected = $data")
                    holder.promptListener(
                        inflater = inflater,
                        safeData = data,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}