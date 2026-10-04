package com.calendario.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.calendario.app.CalendarioApp
import com.calendario.app.data.EventEntity
import com.calendario.app.databinding.FragmentCalendarBinding
import com.calendario.app.sync.SyncRepository
import com.calendario.app.util.ColorUtils
import com.calendario.app.util.Prefs
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _b: FragmentCalendarBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: EventsAdapter

    private var month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var selectedDay: Long = startOfDay(System.currentTimeMillis())
    private var all: List<EventEntity> = emptyList()
    private var colorFilter: Int? = null // null = tutti

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCalendarBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adapter = EventsAdapter { e ->
            EventDialog(Prefs.getCode(requireContext()), selectedDay, e)
                .show(parentFragmentManager, "edit")
        }
        b.eventsList.layoutManager = LinearLayoutManager(requireContext())
        b.eventsList.adapter = adapter

        val saved = Prefs.getCode(requireContext())
        b.codeInput.setText(saved)
        b.connectBtn.setOnClickListener {
            SyncRepository.connect(requireContext(), b.codeInput.text.toString())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            SyncRepository.status.collectLatest { b.syncStatus.text = it }
        }
        if (saved.isNotEmpty()) SyncRepository.startListening(requireContext(), saved)

        b.prevMonth.setOnClickListener { month.add(Calendar.MONTH, -1); drawMonth() }
        b.nextMonth.setOnClickListener { month.add(Calendar.MONTH, 1); drawMonth() }
        b.fabAdd.setOnClickListener {
            val code = Prefs.getCode(requireContext())
            if (code.isEmpty()) {
                b.syncStatus.text = "Prima inserisci un codice condiviso e premi Connetti"
                return@setOnClickListener
            }
            EventDialog(code, selectedDay).show(parentFragmentManager, "add")
        }

        buildColorChips()
        drawHeaders()
        drawMonth()

        viewLifecycleOwner.lifecycleScope.launch {
            (requireActivity().application as CalendarioApp).db.eventDao()
                .observeByCode(saved.ifEmpty { "__none__" })
                .collectLatest { /* ricaricato sotto con code dinamico */ }
        }
        // osserva cambi codice: ricarica lista filtrando per codice corrente
        viewLifecycleOwner.lifecycleScope.launch {
            (requireActivity().application as CalendarioApp).db.eventDao()
                .observeByCode(Prefs.getCode(requireContext()).ifEmpty { "__none__" })
                .collectLatest { /* placeholder */ }
        }
        observeEvents()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            // polling semplice del Flow sul codice corrente (ricreato a ogni resume)
            val dao = (requireActivity().application as CalendarioApp).db.eventDao()
            // Ascolta tutti e filtra in memoria per supportare cambio codice senza ricreare fragment
            kotlinx.coroutines.flow.flow<List<EventEntity>> {
                while (true) {
                    val code = Prefs.getCode(requireContext()).ifEmpty { "__none__" }
                    emit(dao.listByCode(code))
                    kotlinx.coroutines.delay(1000)
                }
            }.collectLatest {
                all = it
                drawMonth()
                drawDayList()
            }
        }
    }

    private fun buildColorChips() {
        b.colorFilter.removeAllViews()
        val tutto = Chip(requireContext()).apply {
            text = "Tutti"; isCheckable = true; isChecked = true
            setOnClickListener { colorFilter = null; clearOthers(this); drawMonth(); drawDayList() }
        }
        b.colorFilter.addView(tutto)
        ColorUtils.names.forEachIndexed { idx, name ->
            val chip = Chip(requireContext()).apply {
                text = name; isCheckable = true
                chipIcon = ColorUtils.dotDrawable(requireContext(), idx)
                setOnClickListener { colorFilter = idx; clearOthers(this); drawMonth(); drawDayList() }
            }
            b.colorFilter.addView(chip)
        }
    }

    private fun clearOthers(keep: Chip) {
        for (i in 0 until b.colorFilter.childCount) {
            val c = b.colorFilter.getChildAt(i) as Chip
            if (c != keep) c.isChecked = false
        }
        keep.isChecked = true
    }

    private fun drawHeaders() {
        val days = listOf("L", "M", "M", "G", "V", "S", "D")
        b.daysHeader.removeAllViews()
        for (d in days) {
            val t = TextView(requireContext()).apply {
                text = d; gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFFB3B3B3.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(2)
                }
            }
            b.daysHeader.addView(t)
        }
    }

    private fun filtered(): List<EventEntity> =
        if (colorFilter == null) all else all.filter { it.colorIndex == colorFilter }

    private fun drawMonth() {
        val list = filtered()
        val byDay = list.groupBy { startOfDay(it.startMillis) }
        val title = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN).format(month.time)
        b.monthTitle.text = title.replaceFirstChar { it.uppercase() }

        b.monthGrid.removeAllViews()
        val first = (month.clone() as Calendar)
        // offset lunedì=0
        val dow = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val dim = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        repeat(dow) { b.monthGrid.addView(emptyCell()) }
        for (day in 1..dim) {
            val cal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val key = startOfDay(cal.timeInMillis)
            val count = byDay[key]?.size ?: 0
            val isSel = key == selectedDay
            val cell = TextView(requireContext()).apply {
                text = if (count > 0) "$day\n•$count" else "$day"
                gravity = Gravity.CENTER
                setPadding(4, 18, 4, 18)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 15f
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    setColor(if (isSel) 0xFF333333.toInt() else 0xFF1E1E1E.toInt())
                    setStroke(2, if (isSel) 0xFFFFFFFF.toInt() else 0xFF2A2A2A.toInt())
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(3, 3, 3, 3)
                }
                setOnClickListener { selectedDay = key; drawMonth(); drawDayList() }
            }
            // pallino colore dominante del giorno
            byDay[key]?.firstOrNull()?.let {
                cell.setCompoundDrawablesWithIntrinsicBounds(null, null, null, ColorUtils.dotDrawable(requireContext(), it.colorIndex))
            }
            b.monthGrid.addView(cell)
        }
    }

    private fun emptyCell(): View = View(requireContext()).apply {
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
    }

    private fun drawDayList() {
        val list = filtered().filter { startOfDay(it.startMillis) == selectedDay }
            .sortedBy { it.startMillis }
        b.selectedDayLabel.text =
            SimpleDateFormat("EEEE dd MMMM", Locale.ITALIAN).format(Date(selectedDay))
                .replaceFirstChar { it.uppercase() } + " • ${list.size} attività"
        adapter.submitList(list)
        b.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun startOfDay(ms: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
