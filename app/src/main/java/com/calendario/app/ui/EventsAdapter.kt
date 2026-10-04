package com.calendario.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calendario.app.data.EventEntity
import com.calendario.app.databinding.ItemEventBinding
import com.calendario.app.util.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(private val onEdit: (EventEntity) -> Unit) :
    ListAdapter<EventEntity, EventsAdapter.VH>(D) {

    companion object {
        val D = object : DiffUtil.ItemCallback<EventEntity>() {
            override fun areItemsTheSame(a: EventEntity, b: EventEntity) = a.id == b.id
            override fun areContentsTheSame(a: EventEntity, b: EventEntity) = a == b
        }
    }

    inner class VH(val b: ItemEventBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(ItemEventBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = getItem(pos)
        val fmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.ITALIAN)
        h.b.title.text = e.title
        h.b.subtitle.text = fmt.format(Date(e.startMillis)) +
            (if (e.description.isNotBlank()) " • ${e.description}" else "")
        h.b.colorBar.setBackgroundColor(ColorUtils.resolve(h.itemView.context, e.colorIndex))
        h.b.editBtn.setOnClickListener { onEdit(e) }
        h.itemView.setOnClickListener { onEdit(e) }
    }
}
