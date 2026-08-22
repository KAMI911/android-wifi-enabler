package com.kami911.wifienabler.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kami911.wifienabler.R
import com.kami911.wifienabler.data.LogEntry
import com.kami911.wifienabler.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.textTimestamp.text = dateFormat.format(Date(entry.timestamp))
            binding.textTrigger.text = entry.trigger
            binding.textAction.text = entry.action.replaceFirstChar { it.uppercase() }
            binding.textMessage.text = entry.message

            val ctx = binding.root.context
            val colorRes = if (entry.success) R.color.log_success else R.color.log_failure
            binding.viewStatusBar.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))
            binding.textAction.setTextColor(ContextCompat.getColor(ctx, colorRes))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LogEntry>() {
            override fun areItemsTheSame(old: LogEntry, new: LogEntry) = old.id == new.id
            override fun areContentsTheSame(old: LogEntry, new: LogEntry) = old == new
        }
    }
}
