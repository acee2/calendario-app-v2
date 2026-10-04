package com.calendario.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.calendario.app.R
import com.calendario.app.data.EventEntity
import com.calendario.app.sync.SyncRepository
import com.calendario.app.util.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

class EventDialog(
    private val calendarCode: String,
    private val initialDay: Long, // mezzanotte del giorno selezionato
    private val existing: EventEntity? = null
) : DialogFragment() {

    private var dateTime: Long = existing?.startMillis ?: (initialDay + 9 * 3600_000L)
    private var colorIdx: Int = existing?.colorIndex ?: 4
    private var reminder: Int = existing?.reminderMinutes ?: 0
    private val reminderOptions = listOf(-1, 0, 15, 60, 1440)
    private val reminderLabels = listOf("Nessuno", "All'ora esatta", "15 min prima", "1 ora prima", "1 giorno prima")

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return i.inflate(R.layout.dialog_event, c, false)
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        val t = v.findViewById<EditText>(R.id.inputTitle)
        val d = v.findViewById<EditText>(R.id.inputDesc)
        val preview = v.findViewById<TextView>(R.id.dateTimePreview)
        val grid = v.findViewById<GridLayout>(R.id.colorGrid)
        val spin = v.findViewById<Spinner>(R.id.reminderSpinner)

        existing?.let { t.setText(it.title); d.setText(it.description) }

        fun refresh() {
            preview.text = SimpleDateFormat("EEEE dd MMM yyyy • HH:mm", Locale.ITALIAN)
                .format(Date(dateTime))
        }
        refresh()

        v.findViewById<Button>(R.id.pickDate).setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = dateTime }
            DatePickerDialog(requireContext(), { _, y, m, day ->
                val c2 = Calendar.getInstance().apply {
                    timeInMillis = dateTime; set(y, m, day)
                }
                dateTime = c2.timeInMillis; refresh()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        v.findViewById<Button>(R.id.pickTime).setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = dateTime }
            TimePickerDialog(requireContext(), { _, h, min ->
                val c2 = Calendar.getInstance().apply {
                    timeInMillis = dateTime
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0)
                }
                dateTime = c2.timeInMillis; refresh()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // pallini colore
        grid.removeAllViews()
        for (idx in 0..7) {
            val b = View(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 88; height = 88
                    setMargins(6, 6, 6, 6)
                }
                background = ColorUtils.dotDrawable(requireContext(), idx)
                alpha = if (idx == colorIdx) 1f else 0.35f
                setOnClickListener {
                    colorIdx = idx
                    for (k in 0 until grid.childCount) grid.getChildAt(k).alpha = 0.35f
                    alpha = 1f
                }
            }
            // bordo selezione
            if (idx == colorIdx) b.alpha = 1f
            grid.addView(b)
        }

        spin.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reminderLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spin.setSelection(reminderOptions.indexOf(reminder).coerceAtLeast(0))
        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, vv: View?, pos: Int, id: Long) {
                reminder = reminderOptions[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val del = v.findViewById<Button>(R.id.deleteBtn)
        if (existing != null) {
            del.visibility = View.VISIBLE
            del.setOnClickListener {
                SyncRepository.deleteEvent(requireContext(), existing)
                dismiss()
            }
        }
        v.findViewById<Button>(R.id.cancelBtn).setOnClickListener { dismiss() }
        v.findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val title = t.text.toString().trim()
            if (title.isEmpty()) { t.error = "Obbligatorio"; return@setOnClickListener }
            val e = (existing?.copy(
                title = title, description = d.text.toString().trim(),
                startMillis = dateTime, colorIndex = colorIdx,
                reminderMinutes = reminder, updatedAt = System.currentTimeMillis()
            ) ?: EventEntity(
                title = title, description = d.text.toString().trim(),
                startMillis = dateTime, colorIndex = colorIdx,
                reminderMinutes = reminder, calendarCode = calendarCode
            ))
            SyncRepository.pushEvent(requireContext(), e)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
