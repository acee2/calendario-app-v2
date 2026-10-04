package com.calendario.app.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val N = "cal_prefs"
    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences(N, Context.MODE_PRIVATE)

    fun getCode(c: Context): String =
        sp(c).getString("code", "") ?: ""

    fun setCode(c: Context, v: String) {
        sp(c).edit().putString("code", v.trim().lowercase()).apply()
    }
}
