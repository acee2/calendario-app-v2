package com.calendario.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.calendario.app.CalendarioApp
import com.calendario.app.data.EventEntity
import com.calendario.app.databinding.FragmentSearchBinding
import com.calendario.app.util.ColorUtils
import com.calendario.app.util.Prefs
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.*

class SearchFragment : Fragment() {

    private var _b: FragmentSearchBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: EventsAdapter
    private var all: List<EventEntity> = emptyList()
    private var colorFilter: Int? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSearchBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        adapter = EventsAdapter { e ->
            EventDialog(Prefs.getCode(requireContext()), e.startMillis, e)
                .show(parentFragmentManager, "edit2")
        }
        b.resultsList.layoutManager = LinearLayoutManager(requireContext())
        b.resultsList.adapter = adapter

        buildChips()
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p: Editable?) = apply()
            override fun beforeTextChanged(a: CharSequence?, x: Int, y: Int, z: Int) {}
            override fun onTextChanged(a: CharSequence?, x: Int, y: Int, z: Int) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            val code = Prefs.getCode(requireContext()).ifEmpty { "__none__" }
            (requireActivity().application as CalendarioApp).db.eventDao()
                .listByCode(code).let {
                    // primo caricamento + polling leggero
                    all = it
                    apply()
                }
            while (true) {
                kotlinx.coroutines.delay(1500)
                val c2 = Prefs.getCode(requireContext()).ifEmpty { "__none__" }
                all = (requireActivity().application as CalendarioApp).db.eventDao().listByCode(c2)
                apply()
            }
        }
    }

    private fun buildChips() {
        b.searchColorFilter.removeAllViews()
        val tutto = Chip(requireContext()).apply {
            text = "Tutti"; isCheckable = true; isChecked = true
            setOnClickListener { colorFilter = null; clearOthers(this); apply() }
        }
        b.searchColorFilter.addView(tutto)
        ColorUtils.names.forEachIndexed { idx, name ->
            b.searchColorFilter.addView(Chip(requireContext()).apply {
                text = name; isCheckable = true
                chipIcon = ColorUtils.dotDrawable(requireContext(), idx)
                setOnClickListener { colorFilter = idx; clearOthers(this); apply() }
            })
        }
    }

    private fun clearOthers(keep: Chip) {
        for (i in 0 until b.searchColorFilter.childCount) {
            val c = b.searchColorFilter.getChildAt(i) as Chip
            if (c != keep) c.isChecked = false
        }
        keep.isChecked = true
    }

    private fun apply() {
        if (_b == null) return
        val q = b.searchInput.text.toString().trim().lowercase(Locale.ITALIAN)
        val res = all.filter {
            (colorFilter == null || it.colorIndex == colorFilter) &&
            (q.isEmpty() || it.title.lowercase().contains(q) || it.description.lowercase().contains(q))
        }.sortedBy { it.startMillis }
        adapter.submitList(res)
        b.emptySearch.visibility = if (res.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
