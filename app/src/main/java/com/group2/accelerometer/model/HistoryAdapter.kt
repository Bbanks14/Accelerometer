package com.group2.accelerometer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.group2.accelerometer.R
import com.group2.accelerometer.models.RecordedSession

class HistoryAdapter(
    private val sessions: List<RecordedSession>,
    private val onItemClick: (RecordedSession) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_session_date)
        val tvSize: TextView = view.findViewById(R.id.tv_session_size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.tvDate.text = session.date
        holder.tvSize.text = session.size

        holder.itemView.setOnClickListener {
            onItemClick(session)
        }
    }

    override fun getItemCount() = sessions.size
}

